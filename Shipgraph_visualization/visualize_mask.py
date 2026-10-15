#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
在运行编辑中调整脚本形参并配置解释器即可
脚本形参形如‘--all’,'--mode overlay'见下方用法
ship_floor_plan.bin 可视化工具
================================================================

用来「看见」App 里那张可通行蒙版到底长什么样 —— 它是个纯二进制文件，
在编辑器里打开只有乱码，这个工具把它渲染成 PNG。

依赖：numpy、Pillow、scipy（Anaconda 自带，无需安装任何东西）

用法
----------------------------------------------------------------
    python visualize_mask.py                    # 渲染蒙版（默认）
    python visualize_mask.py --mode overlay     # 叠在底图上，对比看
    python visualize_mask.py --mode components  # 连通分量着色
    python visualize_mask.py --mode reachable --point 1000,345
    python visualize_mask.py --stats            # 只打印统计，不出图
    python visualize_mask.py --all              # 一次性全出

文件格式（见 MainUtil.kt 的 loadMask）
----------------------------------------------------------------
    偏移 0~3   : width   （大端 int32）
    偏移 4~7   : height  （大端 int32）
    偏移 8~... : width*height 个字节，每字节一个像素
                 1 = 通路，0 = 障碍
"""

import argparse
import os
import struct
import sys
import time

import numpy as np
from PIL import Image, ImageDraw, ImageFont

# Windows 控制台默认是 GBK，直接 print 中文在 Git Bash / Windows Terminal 里会乱码。
# 强制用 UTF-8 输出；若你的 cmd 仍然乱码，先执行一次  chcp 65001
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

# ──────────────────────────── 路径默认值 ────────────────────────────

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(HERE)          # ShipEscape/

DEFAULT_BIN = os.path.join(PROJECT_ROOT, "app", "src", "main", "assets", "ship_floor_plan.bin")
DEFAULT_WEBP = os.path.join(PROJECT_ROOT, "app", "src", "main", "res", "drawable", "ship_floor_plan.webp")

# ──────────────────────────── 配色 ────────────────────────────

C_WALK = (255, 255, 255)      # 通路：白
C_BLOCK = (26, 26, 26)        # 障碍：近黑（便于看清墙线）
C_OVERLAY = (230, 40, 40)     # 叠加层：红
C_OUTSIDE = (245, 245, 245)   # 连通分量图里的"其他区域"


# ──────────────────────────── 字体 ────────────────────────────

def load_font(size=28):
    """找一个能显示中文的字体；找不到就退回 Pillow 默认字体。"""
    candidates = [
        r"C:\Windows\Fonts\msyh.ttc",      # 微软雅黑
        r"C:\Windows\Fonts\msyhbd.ttc",
        r"C:\Windows\Fonts\simhei.ttf",    # 黑体
        r"C:\Windows\Fonts\simsun.ttc",    # 宋体
    ]
    for path in candidates:
        if os.path.exists(path):
            try:
                return ImageFont.truetype(path, size)
            except Exception:
                pass
    return ImageFont.load_default()


# ──────────────────────────── 读取 .bin ────────────────────────────

def load_mask(path):
    """
    读入 .bin，返回 (width, height, walkable)。
    walkable 是 bool 数组，True 表示可通行。
    """
    if not os.path.exists(path):
        sys.exit("找不到文件：%s" % path)

    with open(path, "rb") as f:
        raw = f.read()

    if len(raw) < 8:
        sys.exit("文件太短，连 8 字节文件头都不够：%d 字节" % len(raw))

    width, height = struct.unpack(">ii", raw[:8])     # 大端 int32 ×2
    expected = width * height

    if width <= 0 or height <= 0:
        sys.exit("文件头里的尺寸不合法：%d x %d" % (width, height))

    data = raw[8:]
    if len(data) != expected:
        sys.exit("数据长度不对：头部说 %d x %d = %d 字节，实际只有 %d 字节"
                 % (width, height, expected, len(data)))

    pixels = np.frombuffer(data, dtype=np.uint8).reshape(height, width)
    walkable = (pixels == 1)                          # 1 = 通路

    return width, height, walkable


# ──────────────────────────── 统计 ────────────────────────────

def print_stats(width, height, walkable, bin_path, top_n=8):
    total = width * height
    walk = int(walkable.sum())
    block = total - walk

    print("─" * 62)
    print("  文件      : %s" % bin_path)
    print("  文件大小  : %s 字节" % format(os.path.getsize(bin_path), ","))
    print("  尺寸      : %d x %d" % (width, height))
    print("  格子总数  : %s" % format(total, ","))
    print("─" * 62)
    print("  可通行(1) : %s  (%.2f%%)" % (format(walk, ","), walk * 100.0 / total))
    print("  障碍  (0) : %s  (%.2f%%)" % (format(block, ","), block * 100.0 / total))

    # 顺带检查有没有既不是 0 也不是 1 的杂值
    print("─" * 62)
    return walk, block


def label_components(walkable):
    """
    8 邻域连通分量标记。
    返回 (labels, count)。若 scipy 不可用则返回 (None, 0)。
    """
    try:
        from scipy import ndimage
    except ImportError:
        return None, 0

    # 3x3 全 1 的结构元素 = 标准 8 邻域
    structure = np.ones((3, 3), dtype=np.uint8)
    labels, count = ndimage.label(walkable, structure=structure)
    return labels, count


def component_stats(labels, count, top_n=8):
    """返回按大小降序排列的 [(label, size), ...]"""
    if labels is None or count == 0:
        return []
    sizes = np.bincount(labels.ravel())
    sizes[0] = 0                     # 0 是背景（障碍），排除
    order = np.argsort(sizes)[::-1]
    out = []
    for lab in order[:top_n]:
        if sizes[lab] == 0:
            break
        out.append((int(lab), int(sizes[lab])))
    return out


# ──────────────────────────── 渲染 ────────────────────────────

def render_mask(width, height, walkable):
    """纯蒙版：通路白、障碍黑。"""
    rgb = np.zeros((height, width, 3), dtype=np.uint8)
    rgb[walkable] = C_WALK
    rgb[~walkable] = C_BLOCK
    return Image.fromarray(rgb, "RGB")


def render_overlay(width, height, walkable, webp_path):
    """
    把蒙版半透明叠在底图上 —— 一眼就能看出
    「哪些障碍是真墙、哪些其实是被当成墙的文字」。
    """
    if not os.path.exists(webp_path):
        print("  [!] 找不到底图 %s，退回纯蒙版" % webp_path)
        return render_mask(width, height, walkable)

    base = Image.open(webp_path).convert("RGB")
    if base.size != (width, height):
        print("  [!] 底图尺寸 %s 与蒙版 (%d,%d) 不一致，已缩放底图"
              % (base.size, width, height))
        base = base.resize((width, height), Image.NEAREST)

    base_arr = np.asarray(base).astype(np.float32)

    # 障碍处叠 55% 的红
    alpha = 0.55
    tint = np.array(C_OVERLAY, dtype=np.float32)
    mask3 = (~walkable)[..., None]                    # (h,w,1)
    blended = np.where(mask3, base_arr * (1 - alpha) + tint * alpha, base_arr)

    return Image.fromarray(blended.astype(np.uint8), "RGB")


def render_components(width, height, walkable, labels, count):
    """
    连通分量着色：每个互不连通的区域一个颜色。
    用于发现「主甲板和机舱层其实不连通」这类结构问题。
    """
    rgb = np.zeros((height, width, 3), dtype=np.uint8)
    rgb[~walkable] = C_BLOCK

    if labels is None:
        rgb[walkable] = C_WALK
        return Image.fromarray(rgb, "RGB"), []

    stats = component_stats(labels, count, top_n=12)

    # 用固定的调色板，保证每次跑出来颜色一致
    palette = [
        ( 76, 175,  80),   # 绿    —— 最大分量
        ( 33, 150, 243),   # 蓝
        (255, 152,   0),   # 橙
        (156,  39, 176),   # 紫
        (  0, 188, 212),   # 青
        (255, 193,   7),   # 黄
        (233,  30,  99),   # 粉
        ( 96, 125, 139),   # 蓝灰
    ]
    for i, (lab, _size) in enumerate(stats):
        rgb[labels == lab] = palette[i % len(palette)]

    # 没进前 12 的小分量统一涂灰
    big = set(lab for lab, _ in stats)
    for lab in range(1, count + 1):
        if lab not in big:
            rgb[labels == lab] = (200, 200, 200)

    img = Image.fromarray(rgb, "RGB")
    return img, stats


def render_reachable(width, height, walkable, labels, x, y):
    """
    高亮「从 (x,y) 出发能走到」的全部格子。
    火心扩散出问题时，用这个一眼就能看出火心是不是被困在一个小区域里。
    """
    rgb = np.zeros((height, width, 3), dtype=np.uint8)
    rgb[~walkable] = C_BLOCK
    rgb[walkable] = (215, 215, 215)                   # 其他可通行区：浅灰

    info = {}
    if labels is None:
        # 没有 scipy 就退化成「画个十字标记」
        rgb[walkable] = C_WALK
        info["label"] = -1
        info["size"] = int(walkable.sum())
    else:
        lab = int(labels[y, x])
        info["label"] = lab
        if lab == 0:
            info["size"] = 0
            print("  [!] 点 (%d,%d) 落在障碍上" % (x, y))
        else:
            region = (labels == lab)
            rgb[region] = ( 76, 175,  80)             # 可达区：绿
            info["size"] = int(region.sum())

    img = Image.fromarray(rgb, "RGB")

    # 标出起点
    draw = ImageDraw.Draw(img)
    r = 14
    draw.ellipse([x - r, y - r, x + r, y + r], outline=(255, 0, 0), width=5)
    draw.line([x - r * 2, y, x + r * 2, y], fill=(255, 0, 0), width=3)
    draw.line([x, y - r * 2, x, y + r * 2], fill=(255, 0, 0), width=3)

    return img, info


def annotate(img, lines, font_size=30):
    """在图片左上角画一块半透明的说明框。"""
    font = load_font(font_size)
    img = img.convert("RGB")
    draw = ImageDraw.Draw(img, "RGBA")

    pad = 16
    line_h = font_size + 10
    # 估算文字宽度
    max_w = 0
    for line in lines:
        try:
            bbox = draw.textbbox((0, 0), line, font=font)
            max_w = max(max_w, bbox[2] - bbox[0])
        except Exception:
            max_w = max(max_w, len(line) * font_size // 2)

    box = [pad, pad, pad + max_w + pad * 2, pad + line_h * len(lines) + pad]
    draw.rectangle(box, fill=(0, 0, 0, 170))

    for i, line in enumerate(lines):
        draw.text((pad * 2, pad + pad // 2 + i * line_h), line,
                  fill=(255, 255, 255), font=font)

    return img


# ──────────────────────────── 主流程 ────────────────────────────

def main():
    ap = argparse.ArgumentParser(
        description="ship_floor_plan.bin 可视化工具",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__.split("用法")[1] if "用法" in __doc__ else None,
    )
    ap.add_argument("--bin", default=DEFAULT_BIN, help="蒙版文件路径")
    ap.add_argument("--webp", default=DEFAULT_WEBP, help="底图路径（overlay 模式用）")
    ap.add_argument("--out", default=HERE, help="输出目录")
    ap.add_argument("--mode", default="mask",
                    choices=["mask", "overlay", "components", "reachable"],
                    help="渲染模式")
    ap.add_argument("--point", default="1000,345",
                    help="reachable 模式的起点，格式 X,Y")
    ap.add_argument("--stats", action="store_true", help="打印统计信息")
    ap.add_argument("--all", action="store_true", help="一次生成全部模式")
    args = ap.parse_args()

    t0 = time.time()
    width, height, walkable = load_mask(args.bin)
    print("已载入蒙版：%d x %d，耗时 %.2f 秒" % (width, height, time.time() - t0))

    walk, block = print_stats(width, height, walkable, args.bin)

    # 连通分量（components 和 reachable 模式要用）
    need_labels = args.all or args.mode in ("components", "reachable")
    labels, count = (None, 0)
    if need_labels:
        t1 = time.time()
        labels, count = label_components(walkable)
        if labels is None:
            print("  [!] 没装 scipy，连通分量功能不可用（只影响 components/reachable 模式）")
        else:
            print("  连通分量  : %d 个（8 邻域），标记耗时 %.2f 秒"
                  % (count, time.time() - t1))
            stats = component_stats(labels, count, top_n=5)
            print("─" * 62)
            print("  最大的几个连通区域：")
            for i, (lab, size) in enumerate(stats):
                print("    #%d  标签 %-4d  %12s 格  (占可通行区的 %5.1f%%)"
                      % (i + 1, lab, format(size, ","), size * 100.0 / max(walk, 1)))

            # 主甲板/机舱层的分离情况 —— 直接看有几个「大块」
            big = [s for _, s in stats if s > walk * 0.05]
            if len(big) > 1:
                print("─" * 62)
                print("  ⚠ 存在 %d 个各占 5%% 以上的独立区域 —— 它们互相走不通。" % len(big))
                print("     （双层甲板画在同一张图上时，这是预期现象）")
    print("─" * 62)

    if args.stats and not args.all:
        return

    os.makedirs(args.out, exist_ok=True)
    modes = ["mask", "overlay", "components", "reachable"] if args.all else [args.mode]

    font = load_font(30)

    for mode in modes:
        if mode == "mask":
            img = render_mask(width, height, walkable)
            img = annotate(img, [
                "可通行性蒙版 (ship_floor_plan.bin)",
                "白 = 可通行 (1)   %s 格  %.1f%%" % (format(walk, ","), walk * 100.0 / (walk + block)),
                "黑 = 障碍   (0)   %s 格  %.1f%%" % (format(block, ","), block * 100.0 / (walk + block)),
            ], font_size=30)
            name = "mask.png"

        elif mode == "overlay":
            img = render_overlay(width, height, walkable, args.webp)
            img = annotate(img, [
                "蒙版叠加底图（红色 = 被判为障碍的像素）",
                "对照底图即可看出：哪些红色是真墙，",
                "哪些其实是被亮度阈值误判的文字/标注",
            ], font_size=30)
            name = "overlay.png"

        elif mode == "components":
            img, stats = render_components(width, height, walkable, labels, count)
            lines = ["连通分量（8 邻域），共 %d 个" % count]
            for i, (lab, size) in enumerate(stats[:4]):
                lines.append("  #%d  %s 格  %.1f%%" % (i + 1, format(size, ","),
                                                       size * 100.0 / max(walk, 1)))
            if labels is None:
                lines = ["连通分量不可用（缺少 scipy）"]
            img = annotate(img, lines, font_size=28)
            name = "components.png"

        elif mode == "reachable":
            try:
                px, py = [int(v) for v in args.point.split(",")]
            except Exception:
                sys.exit("--point 格式应为 X,Y，例如 1000,345")
            if not (0 <= px < width and 0 <= py < height):
                sys.exit("--point (%d,%d) 超出图片范围 %dx%d" % (px, py, width, height))

            img, info = render_reachable(width, height, walkable, labels, px, py)
            pct = info["size"] * 100.0 / max(walk, 1)
            img = annotate(img, [
                "从 (%d, %d) 出发可达的区域" % (px, py),
                "可达 %s 格，占全部可通行区的 %.1f%%" % (format(info["size"], ","), pct),
                "绿 = 能走到   浅灰 = 走不到   黑 = 障碍",
            ], font_size=30)
            name = "reachable_%d_%d.png" % (px, py)

        else:
            continue

        out_path = os.path.join(args.out, name)
        img.save(out_path)
        print("  已写出 %s" % out_path)

    print("─" * 62)
    print("全部完成，总耗时 %.2f 秒" % (time.time() - t0))


if __name__ == "__main__":
    main()
