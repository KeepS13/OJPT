from __future__ import annotations

import json
import os
import sys
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class FigureSpec:
    fig_no: str
    fig_name: str
    out_file: str
    dot: str


REPO_ROOT = Path(__file__).resolve().parents[2]
FIG_DIR = REPO_ROOT / ".docx" / "论文" / "assets" / "figures"


def _require_graphviz_dot_available() -> None:
    """
    Python graphviz 只负责拼装 DOT 与调用本地 Graphviz 的 dot 可执行程序。
    如果系统没装 Graphviz（dot 不在 PATH），这里给出明确提示。
    """
    try:
        from graphviz import Digraph  # noqa: F401
    except Exception as e:  # pragma: no cover
        raise RuntimeError(
            "缺少 Python 依赖 graphviz。请先执行：python -m pip install graphviz"
        ) from e

    # graphviz 库在 render 时才会真正调用 dot，这里先做 PATH 级别的快速判断
    from shutil import which

    if which("dot") is None:
        raise RuntimeError(
            "未检测到 Graphviz 的 dot 可执行程序。\n"
            "请安装 Graphviz 并把 dot 加入 PATH，然后重新运行：\n"
            "  - Windows：安装 Graphviz（https://graphviz.org/download/）\n"
            "  - 安装后重新打开终端，确认 `where dot` 有输出\n"
        )


def _render_matplotlib_flow(
    out_path: Path,
    nodes: dict[str, tuple[float, float, str]],
    edges: list[tuple[str, str, str]],
    *,
    figsize: tuple[float, float] = (10, 2.6),
    box_w: float = 0.16,
    box_h: float = 0.22,
    font_size: float = 10.5,
) -> None:
    """
    在未安装 Graphviz dot 的情况下，使用 matplotlib 生成简洁的流程图 PNG。
    nodes: id -> (x, y, label)
    edges: (src_id, dst_id, label)
    """
    import matplotlib.pyplot as plt
    from matplotlib import font_manager
    from matplotlib.font_manager import FontProperties
    from matplotlib.patches import FancyBboxPatch

    def pick_font() -> FontProperties:
        # Windows 常见中文字体优先级
        candidates = [
            "Microsoft YaHei",
            "SimHei",
            "Noto Sans CJK SC",
            "PingFang SC",
            "STSong",
            "Arial Unicode MS",
        ]
        for name in candidates:
            try:
                path = font_manager.findfont(FontProperties(family=name), fallback_to_default=False)
                if path:
                    return FontProperties(fname=path)
            except Exception:
                continue
        return FontProperties()  # fallback

    font = pick_font()

    # 用更合理的画布比例，避免“超宽超扁”
    fig = plt.figure(figsize=figsize, dpi=220, facecolor="white")
    ax = fig.add_subplot(111)
    ax.set_axis_off()
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)

    def draw_box(x: float, y: float, text: str) -> None:
        rect = FancyBboxPatch(
            (x - box_w / 2, y - box_h / 2),
            box_w,
            box_h,
            boxstyle="round,pad=0.02,rounding_size=0.02",
            linewidth=1.2,
            edgecolor="#333333",
            facecolor="#f7f7f7",
        )
        ax.add_patch(rect)
        ax.text(
            x,
            y,
            text,
            ha="center",
            va="center",
            fontsize=font_size,
            wrap=True,
            fontproperties=font,
        )

    for _id, (x, y, label) in nodes.items():
        draw_box(x, y, label)

    for src, dst, label in edges:
        x1, y1, _ = nodes[src]
        x2, y2, _ = nodes[dst]
        ax.annotate(
            "",
            xy=(x2, y2),
            xytext=(x1, y1),
            arrowprops=dict(arrowstyle="->", lw=1.2, color="#333333"),
        )
        if label:
            ax.text(
                (x1 + x2) / 2,
                (y1 + y2) / 2 + 0.10,
                label,
                ha="center",
                va="center",
                fontsize=9.5,
                fontproperties=font,
            )

    out_path.parent.mkdir(parents=True, exist_ok=True)
    fig.tight_layout(pad=0.1)
    fig.savefig(out_path, bbox_inches="tight", pad_inches=0.08)
    plt.close(fig)


def _render_fallback_png(spec: FigureSpec, out_path: Path) -> None:
    # 这里用“固定布局”确保可复现与可控，满足论文插图需要。
    if spec.fig_no == "4-1":
        nodes = {
            "User": (0.12, 0.55, "用户"),
            "FE": (0.34, 0.55, "前端\nVue3 TS Element Plus"),
            "BE": (0.56, 0.55, "后端\nSpring Boot"),
            "DB": (0.82, 0.70, "数据库\n题目 标签 状态"),
            "Redis": (0.82, 0.40, "Redis\n令牌 黑名单"),
        }
        edges = [
            ("User", "FE", ""),
            ("FE", "BE", ""),
            ("BE", "DB", ""),
            ("BE", "Redis", ""),
        ]
        _render_matplotlib_flow(out_path, nodes, edges, figsize=(10.5, 2.8))
        return

    if spec.fig_no == "4-2":
        nodes = {
            "AdminLogin": (0.10, 0.55, "管理员登录"),
            "DraftList": (0.30, 0.55, "进入草稿池\n或已发布列表"),
            "Edit": (0.50, 0.55, "题目编辑\n左右分栏预览"),
            "Publish": (0.70, 0.55, "发布\nPUBLISHED"),
            "Visible": (0.88, 0.55, "学员端可见\n列表与详情"),
            "Archive": (0.88, 0.25, "归档\nARCHIVED"),
        }
        edges = [
            ("AdminLogin", "DraftList", ""),
            ("DraftList", "Edit", ""),
            ("Edit", "Publish", ""),
            ("Publish", "Visible", ""),
            ("Visible", "Archive", ""),
        ]
        _render_matplotlib_flow(out_path, nodes, edges, figsize=(12.0, 2.8), box_w=0.15)
        return

    if spec.fig_no == "5-1":
        # 节点精简，避免横向过多导致重叠
        nodes = {
            "List": (0.10, 0.55, "题库列表\nproblemNo 路由"),
            "Detail": (0.30, 0.55, "题目详情\nMarkdown 渲染"),
            "LangTpl": (0.50, 0.55, "选择语言\n应用 Main 模板"),
            "Edit": (0.70, 0.55, "编辑代码\n行号与滚动"),
            "Submit": (0.90, 0.55, "提交\n队列反馈"),
        }
        edges = [
            ("List", "Detail", ""),
            ("Detail", "LangTpl", ""),
            ("LangTpl", "Edit", ""),
            ("Edit", "Submit", ""),
        ]
        _render_matplotlib_flow(out_path, nodes, edges, figsize=(10.8, 2.6), box_w=0.16)
        return

    if spec.fig_no == "5-2":
        nodes = {
            "Md": (0.12, 0.55, "statementMd\nMarkdown 文本"),
            "Marked": (0.34, 0.55, "marked\n解析"),
            "Purify": (0.56, 0.55, "DOMPurify\n清洗"),
            "Render": (0.82, 0.55, "HTML 输出\n页面渲染"),
        }
        edges = [
            ("Md", "Marked", ""),
            ("Marked", "Purify", ""),
            ("Purify", "Render", ""),
        ]
        _render_matplotlib_flow(out_path, nodes, edges, figsize=(9.8, 2.6), box_w=0.18)
        return

    if spec.fig_no == "5-3":
        nodes = {
            "Start": (0.12, 0.80, "准备\n启动服务\n初始化数据"),
            "Admin": (0.12, 0.48, "发布可见性\n草稿→发布→可见"),
            "Ban": (0.50, 0.48, "封禁提示一致性\n剩余时间展示"),
            "Page": (0.88, 0.48, "分页指标回归\ntotal pages size"),
            "Assert": (0.50, 0.18, "断言\n页面与接口契约一致"),
        }
        edges = [
            ("Start", "Admin", ""),
            ("Start", "Ban", ""),
            ("Start", "Page", ""),
            ("Admin", "Assert", ""),
            ("Ban", "Assert", ""),
            ("Page", "Assert", ""),
        ]
        _render_matplotlib_flow(out_path, nodes, edges, figsize=(10.6, 3.2), box_w=0.18, box_h=0.24, font_size=10.0)
        return

    raise ValueError(f"未知图号：{spec.fig_no}")


def _specs() -> list[FigureSpec]:
    # 说明：这里用 DOT 文本而不是构建复杂对象，便于后续人工微调布局。
    return [
        FigureSpec(
            fig_no="4-1",
            fig_name="系统总体架构图",
            out_file="fig-4-1-architecture.png",
            dot=r"""
digraph G {
  rankdir=LR;
  node [shape=box, fontname="Microsoft YaHei"];

  User [label="用户"];
  FE [label="前端\nVue3 + TS + Element Plus"];
  BE [label="后端\nSpring Boot"];
  DB [label="数据库\n题目 标签 状态", shape=cylinder];
  Redis [label="Redis\n令牌 黑名单", shape=cylinder];

  User -> FE [label="浏览 做题 管理"];
  FE -> BE [label="HTTP API"];
  BE -> DB [label="查询 写入"];
  BE -> Redis [label="鉴权 缓存"];
}
""".strip(),
        ),
        FigureSpec(
            fig_no="4-2",
            fig_name="管理端题目发布归档业务流程图",
            out_file="fig-4-2-admin-flow.png",
            dot=r"""
digraph G {
  rankdir=LR;
  node [shape=box, fontname="Microsoft YaHei"];

  AdminLogin [label="管理员登录"];
  DraftList [label="进入草稿池或已发布列表"];
  EditProblem [label="题目编辑\nMarkdown 左右分栏预览"];
  Publish [label="发布\nstatus=PUBLISHED"];
  Visible [label="学员端可见\n列表与详情展示"];
  Archive [label="归档\nstatus=ARCHIVED"];

  AdminLogin -> DraftList -> EditProblem -> Publish -> Visible;
  Visible -> Archive;
}
""".strip(),
        ),
        FigureSpec(
            fig_no="5-1",
            fig_name="学员端做题与提交链路流程图",
            out_file="fig-5-1-solve-submit.png",
            dot=r"""
digraph G {
  rankdir=LR;
  node [shape=box, fontname="Microsoft YaHei"];

  List [label="题库列表\nproblemNo 路由"];
  Detail [label="题目详情\nMarkdown 渲染"];
  ChooseLang [label="选择语言\nC/C++ Java Python3"];
  Template [label="应用入口模板\nMain 文件骨架"];
  Edit [label="编辑代码\n行号与滚动"];
  Submit [label="提交"];
  Feedback [label="反馈\n进入评测队列"];

  List -> Detail -> ChooseLang -> Template -> Edit -> Submit -> Feedback;
}
""".strip(),
        ),
        FigureSpec(
            fig_no="5-2",
            fig_name="Markdown 渲染与安全清洗处理流程图",
            out_file="fig-5-2-md-sanitize.png",
            dot=r"""
digraph G {
  rankdir=LR;
  node [shape=box, fontname="Microsoft YaHei"];

  Md [label="statementMd\nMarkdown 文本", shape=note];
  Marked [label="marked\n解析"];
  Purify [label="DOMPurify\n清洗"];
  Html [label="HTML 输出"];
  Render [label="页面渲染\n统一样式"];

  Md -> Marked -> Purify -> Html -> Render;
}
""".strip(),
        ),
        FigureSpec(
            fig_no="5-3",
            fig_name="端到端测试关键回归点覆盖图",
            out_file="fig-5-3-e2e.png",
            dot=r"""
digraph G {
  rankdir=TB;
  node [shape=box, fontname="Microsoft YaHei"];

  Start [label="准备\n启动前后端 初始化数据"];
  AdminFlow [label="管理端发布可见性\n草稿->发布->学员端可见"];
  BanFlow [label="封禁登录提示一致性\n错误结构与剩余时间"];
  PageFlow [label="分页指标回归\ntotal pages size 正确"];
  Assert [label="断言\n页面与接口契约一致"];

  Start -> AdminFlow -> Assert;
  Start -> BanFlow -> Assert;
  Start -> PageFlow -> Assert;
}
""".strip(),
        ),
    ]


def main() -> int:
    FIG_DIR.mkdir(parents=True, exist_ok=True)

    dot_available = True
    try:
        _require_graphviz_dot_available()
    except Exception as e:
        dot_available = False
        print(str(e), file=sys.stderr)
        print("将使用 matplotlib 生成简洁版 PNG 作为替代。", file=sys.stderr)

    specs = _specs()
    manifest = []

    for spec in specs:
        out_path = FIG_DIR / spec.out_file
        if dot_available:
            from graphviz import Source

            src = Source(spec.dot, format="png", engine="dot")
            # graphviz.render 会自动加后缀，给一个“无后缀”的 stem 更直观
            tmp_stem = out_path.with_suffix("")
            rendered = Path(src.render(filename=str(tmp_stem), cleanup=True))
            if rendered != out_path:
                # render 返回的是实际生成文件路径，这里确保命名一致
                if out_path.exists():
                    out_path.unlink()
                rendered.replace(out_path)
        else:
            _render_fallback_png(spec, out_path)

        manifest.append(
            {
                "fig_no": spec.fig_no,
                "fig_name": spec.fig_name,
                "file": str(out_path.relative_to(REPO_ROOT)).replace(os.sep, "/"),
            }
        )

    (FIG_DIR / "figures.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print(f"已生成 {len(specs)} 张图到：{FIG_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

