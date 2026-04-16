from __future__ import annotations

from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
THESIS_DIR = REPO_ROOT / ".docx" / "论文"
OUT_FILE = THESIS_DIR / "OJPT-论文-合并版.md"


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8").strip() + "\n"


def main() -> int:
    inputs = [
        THESIS_DIR / "00-封面.md",
        THESIS_DIR / "00.5-原创性声明.md",
        THESIS_DIR / "03-目录.md",
        THESIS_DIR / "01-摘要.md",
        THESIS_DIR / "04-英文摘要-Abstract.md",
        THESIS_DIR / "10-第1章-绪论.md",
        THESIS_DIR / "20-第2章-相关技术和理论概述.md",
        THESIS_DIR / "30-第3章-系统需求分析.md",
        THESIS_DIR / "40-第4章-系统分析与设计.md",
        THESIS_DIR / "50-第5章-系统的开发设计与实现.md",
        THESIS_DIR / "60-第6章-结论.md",
        THESIS_DIR / "90-参考文献.md",
        THESIS_DIR / "91-致谢.md",
    ]

    missing = [p for p in inputs if not p.exists()]
    if missing:
        msg = "\n".join(str(p.relative_to(REPO_ROOT)) for p in missing)
        raise FileNotFoundError(f"以下文件不存在，无法合并：\n{msg}")

    parts: list[str] = []
    parts.append("<!-- merged-by: scripts/thesis/merge_thesis.py -->\n")

    for p in inputs:
        parts.append(_read_text(p))
        parts.append("\n---\n\n")

    OUT_FILE.write_text("".join(parts).rstrip() + "\n", encoding="utf-8")
    print(f"已输出合并版：{OUT_FILE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

