export type SupportedLanguage = 'C/C++' | 'Java' | 'Python3'

export interface LanguageTemplate {
  language: SupportedLanguage
  /**
   * 入口文件名，仅作为沙盒/提交元信息使用
   * 约定：所有语言主入口文件名均为 Main.*
   */
  mainFileName: string
  /**
   * 模板正文（完整 Main 文件骨架）
   */
  template: string
}

export const defaultLanguageTemplates: Record<SupportedLanguage, LanguageTemplate> = {
  'C/C++': {
    language: 'C/C++',
    mainFileName: 'Main.cpp',
    template: `#include <bits/stdc++.h>
using namespace std;

int main() {
    // 在这里写你的代码
    return 0;
}
`,
  },
  Java: {
    language: 'Java',
    mainFileName: 'Main.java',
    template: `public class Main {
    public static void main(String[] args) throws Exception {
        // 在这里写你的代码
    }
}
`,
  },
  Python3: {
    language: 'Python3',
    mainFileName: 'main.py',
    template: `def main() -> None:
    # 在这里写你的代码
    pass


if __name__ == "__main__":
    main()
`,
  },
}

