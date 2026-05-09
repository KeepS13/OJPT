# 武汉工商学院本科毕业论文（设计）封面

（字体：小三；加粗；居中）

论文题目：在线判题平台设计与实现

（字体：小四；居中）

学号：__________
学生姓名：__________
学院：__________
专业：__________
年级：202__级
指导教师：__________（职称：__________）

（字体：四号；居中）

二〇二六年___月

<!-- pagebreak -->

# 原创性声明

（字体：宋体；字号：小二；加粗；段落格式：单倍行距；居中）

本人郑重声明：所呈交的论文是本人在指导教师的指导下独立完成研究工作所取得的成果。除文中特别加以标注引用的内容外，本论文不包含任何其他个人或集体已经发表或撰写的成果作品。本人的论文对相关数据、观点与研究过程具有真实性、完整性与可追溯性，本人完全意识到本声明的法律后果由本人承担。

（宋体；字号：四号；段落格式：单倍行距；首行缩进2字符）

作者签名：__________

年  月  日

<!-- pagebreak -->

# 目录

（页码占位：导出Word后按模板自动更新）

摘  要 ........................................................ 1

关键词 ........................................................ 2

Abstract ....................................................... 3

Key words ...................................................... 4

1绪论 ........................................................ 5

1.1研究背景与意义 ............................................. 5

1.2国内外研究现状 ............................................. 6

1.3研究内容 ................................................... 7

1.4技术路线与研究方法 ......................................... 8

2相关技术和理论概述 ........................................ 10

2.1在线判题系统概述 .......................................... 10

2.2 SpringBoot与后端分层开发 ............................... 11

2.3 Vue 3与前端工程化 ........................................ 12

2.4 MyBatis-Plus、MySQL与Redis ............................. 13

2.5 JWT鉴权与RBAC权限控制 .................................. 14

2.6 Docker判题环境 ............................................ 15

2.7 Markdown渲染与内容安全 .................................... 16

2.8自动化测试技术 ............................................. 17


3系统需求分析 ................................................ 18

3.1系统功能需求分析 ............................................ 18

3.1.1系统主要功能介绍 .......................................... 18

3.1.2系统功能模块分析 .......................................... 19

3.1.3系统用例分析 .............................................. 20

3.2系统功能流程介绍 ............................................ 21

3.2.1系统数据流分析 ............................................ 21

3.2.2数据字典 ................................................ 23


4系统分析与设计 .............................................. 31

4.1系统总体架构设计 .......................................... 31

4.2系统部署结构设计 .......................................... 32

4.3数据库概念结构设计 ........................................ 33

4.4概念模型向关系模型的转换 .................................. 38

4.5数据库表的设计 ............................................ 39


5系统的开发设计与实现 .......................................... 46

5.1用户登录功能的设计与实现 .................................... 46

5.2用户注册功能的设计与实现 .................................... 47

5.3题库浏览功能的设计与实现 .................................... 48

5.4在线做题功能的设计与实现 .................................... 49

5.5代码草稿功能的设计与实现 .................................... 50

5.6代码运行功能的设计与实现 .................................... 51

5.7提交判题功能的设计与实现 .................................... 52

5.8提交结果查询功能的设计与实现 ................................ 53

5.9个人中心与训练看板功能的设计与实现 .......................... 54

5.10后台题目管理功能的设计与实现 ............................... 55

5.11后台用户管理功能的设计与实现 ............................... 56

5.12判题环境检测功能的设计与实现 ............................... 57


6系统测试 .................................................... 59

6.1测试方法 .................................................. 59

6.2测试环境 .................................................. 60

6.3功能测试 .................................................. 61

6.4接口测试 .................................................. 64

6.5判题流程测试 .............................................. 66

6.6权限测试 .................................................. 67

6.7判题环境检测测试 .......................................... 68

6.8自动化测试与测试结果分析 .................................. 69


7总结与展望 .................................................. 69

参考文献 ...................................................... 72

致谢 .......................................................... 73

<!-- pagebreak -->

# 摘要

随着程序设计课程实践、算法训练和编程竞赛活动的持续开展，在线判题系统在代码评测、结果反馈和训练记录管理方面具有重要作用。传统人工评阅方式难以及时处理大量代码提交，也不便于持续跟踪学习者的训练情况。针对个人算法训练和题库维护需求，本文设计并实现了OJPT在线编程训练平台，为用户提供在线练习、代码评测和训练数据查看等功能，为管理员提供题库和判题环境维护能力。

本系统采用前后端分离架构，后端使用SpringBoot、Spring Security、MyBatis-Plus、MySQL、Redis和Docker等技术实现业务接口、权限控制、数据持久化和代码隔离执行，前端使用Vue 3、TypeScript、Element Plus、Pinia和Axios构建交互页面。系统采用JWT进行身份认证，通过角色权限控制区分普通用户和管理员操作，并利用Docker隔离执行不同语言的用户代码。

系统主要实现了用户登录注册、题库浏览、在线做题、代码运行、提交判题、提交结果查询、个人中心与训练看板、后台用户管理、后台题目与测试用例维护和判题环境检测等功能。经过功能测试、接口测试、判题流程测试、权限测试、判题环境检测和自动化测试，结果表明系统主要功能能够正常运行，满足毕业设计阶段对在线判题平台功能实现和测试验证的要求。

关键词：在线判题；SpringBoot；Vue 3；Docker判题；权限控制

<!-- pagebreak -->

# 关键词

在线判题；SpringBoot；Vue 3；Docker判题；权限控制

<!-- pagebreak -->

# Abstract

With the continuous development of programming practice, algorithm training, and programming contests, online judge systems play an important role in code evaluation, result feedback, and training record management. Manual code review is inefficient when submissions increase, and it is difficult to track learners' training progress continuously. To meet the needs of personal algorithm training and problem bank maintenance, this thesis designs and implements OJPT, an online programming training platform that provides online practice, code evaluation, and training data viewing for users, as well as problem bank and judge environment maintenance for administrators.

OJPT adopts a frontend-backend separation architecture. The backend uses SpringBoot, Spring Security, MyBatis-Plus, MySQL, Redis, and Docker to implement business APIs, permission control, data persistence, and isolated code execution. The frontend uses Vue 3, TypeScript, Element Plus, Pinia, and Axios to build interactive pages. JWT is used for identity authentication, role-based access control is used to separate normal user operations from administrator operations, and Docker is used to execute user code in an isolated environment.

The system implements user login and registration, problem browsing, online solving, code running, judging submission, submission result query, user center and training dashboard, administrator user management, problem and test case maintenance, and judge environment checking. Through functional testing, API testing, judging process testing, permission testing, judge environment checking, and automated testing, the results show that the main functions of the system run properly and meet the graduation project requirements for online judge function implementation and test verification.

Key words: online judge; SpringBoot; Vue 3; Docker judging; access control

<!-- pagebreak -->

# 1绪论

## 1.1研究背景与意义

程序设计能力培养通常依赖持续练习、即时反馈和阶段性总结。在线判题系统能够把题目描述、代码提交、自动运行、结果反馈和训练统计整合到同一平台中，使学习者在完成题目后及时获得结果，并能够通过提交记录追踪自己的训练情况。对于管理员而言，在线判题平台也能够统一维护题目、测试用例、标签和用户状态，减少题库分散管理带来的维护成本。

在传统人工评阅模式下，教师或管理人员需要手动下载代码、准备输入输出、运行程序并记录结果。该方式不仅效率低，而且难以保证评测环境的一致性。当练习人数、题目数量和提交次数增加时，人工方式很难持续支撑。在线判题系统通过标准输入输出、资源限制、容器隔离和结构化结果存储，能够提高评测效率，并为后续训练统计和题库维护提供数据基础。

本文设计并实现的OJPT在线编程训练平台主要面向个人算法训练场景。系统不把课程、班级和作业批改作为当前主线，而是聚焦普通用户做题训练和管理员题库维护两类核心需求。普通用户可以浏览题库、筛选题目、进入做题页、运行样例或自定义用例、正式提交代码并查看提交记录和训练看板；管理员可以维护用户、题目、测试用例、标签、统计数据和判题环境。该系统能够为本科毕业设计提供一个功能完整、可演示、可验证的在线判题平台样例。

本课题的意义主要体现在三个方面。第一，系统从实际训练流程出发，覆盖题库浏览、代码编辑、运行判题和训练数据查看等环节，能够反映在线判题平台的典型业务流程。第二，系统通过Docker运行用户代码，在一定程度上隔离用户程序与后端服务环境，为后续扩展更严格的安全沙盒提供基础。第三，系统按照软件工程方法整理需求分析、数据流图、实体属性图、E-R图、业务流程图和测试用例，使项目实现和论文材料之间形成对应关系。

## 1.2国内外研究现状

在线判题系统已经广泛应用于程序设计教学、算法竞赛和企业编程能力训练。典型平台通常包含题库管理、用户账号、代码编辑、编译运行、结果评测、提交历史、排行榜和统计分析等功能。成熟系统往往更加关注大规模并发评测、复杂权限体系、多语言沙盒、安全隔离和竞赛组织能力[1-4]。

从教学和训练角度看，在线判题系统的关键不只是“能运行代码”，还包括题面表达是否清晰、测试用例是否便于维护、提交结果是否容易理解、历史记录是否可追踪、管理员是否能够高效维护题库。对于本科阶段项目而言，系统规模不需要完全对标大型竞赛平台，但需要把核心链路做完整，使用户能够从题库入口顺利完成一次训练，并使管理员能够维护训练内容。

当前许多在线判题、在线考试和在线训练类项目采用前后端分离架构。后端负责认证授权、题目数据、提交数据和判题调度，前端负责题库展示、代码编辑和交互反馈。判题环境通常使用容器或隔离进程执行用户代码，避免用户程序直接影响业务服务。数据库保存题目、测试用例、提交记录和用户进度，缓存系统用于会话状态、令牌黑名单或临时数据维护[5-8]。

结合本项目实际，OJPT选择以SpringBoot作为后端框架，以Vue 3作为前端框架，以MySQL作为主要数据存储，以Redis辅助登录令牌管理，以Docker支撑代码运行和判题环境检查。系统功能范围控制在个人训练和后台维护两条主线上，避免把历史遗留的学校、院系、班级模型写成当前系统主功能，从而保证论文描述与实际系统一致。

## 1.3研究内容

本文围绕OJPT在线编程训练平台的设计与实现展开，主要研究内容包括以下几个方面。

第一，完成普通用户训练流程设计与实现。普通用户可以进入首页和题库列表，按照题号、关键词、难度、标签和完成状态筛选题目；进入题目详情后，可以阅读Markdown题面和样例测试用例；登录后可以选择C/C++、Java、Python3等语言，编辑代码，保存代码草稿，运行样例或自定义用例，并进行正式提交。

第二，完成判题与提交结果流程设计与实现。系统把“运行代码”和“正式提交”区分为两个流程。运行代码主要服务于用户调试，可以使用样例或自定义输入；正式提交会创建提交记录，进入后端判题流程，执行测试用例并保存提交状态、运行耗时、内存、错误信息和逐用例结果。前端通过查询提交详情展示最终结果。

第三，完成用户中心和训练数据功能。用户登录后可以查看和修改个人资料、上传或删除头像、修改用户名、邮箱、手机号和密码，也可以查看个人提交记录和训练看板。训练看板展示已解决题数、提交次数、通过率、难度分布、状态分布和近期提交记录，使用户能够了解自己的训练情况。

第四，完成管理员维护功能。管理员可以进入后台查看统计概览和判题环境状态，维护用户列表、用户资料、用户状态和密码重置申请，也可以维护题目、测试用例、标签以及题目与标签的绑定关系。题目状态包括草稿、已发布和已归档，管理员可以通过发布和归档控制题目在普通用户题库中的可见性。

第五，完成软件工程建模和测试验证。本文根据系统实际功能绘制功能模块图、系统用例图、顶层数据流图、零层数据流图、一层数据流图、部署结构图、实体属性图和数据库E-R图，并通过功能测试、接口测试、权限测试、判题流程测试和自动化测试验证系统主要功能。

<!-- pagebreak -->

# 2相关技术和理论概述

表2-1系统主要技术栈表

| 层次 | 技术 | 在系统中的作用 |
| --- | --- | --- |
| 后端框架 | SpringBoot | 提供REST API、配置管理和应用启动能力 |
| 安全认证 | Spring Security、JWT | 实现登录认证、令牌解析和权限控制 |
| 数据访问 | MyBatis-Plus、MyBatis | 实现CRUD、分页查询和SQL映射 |
| 数据库 | MySQL | 保存用户、题目、提交、测试用例等核心数据 |
| 缓存 | Redis | 保存刷新令牌、令牌黑名单等临时状态 |
| 数据迁移 | Flyway | 管理数据库表结构和初始化数据 |
| 前端框架 | Vue 3、TypeScript | 构建单页应用和类型化交互逻辑 |
| UI组件 | Element Plus | 提供表格、表单、弹窗、分页等组件 |
| 判题环境 | Docker | 隔离执行用户代码并收集运行结果 |
| 测试工具 | JUnit、Vitest、Playwright | 覆盖后端、前端和端到端测试 |

## 2.1在线判题系统概述

在线判题系统（Online Judge，OJ）是一类面向程序设计训练和自动化评测的软件系统。用户阅读题目后提交源代码，系统根据预设测试用例编译运行程序，并比较实际输出与期望输出，最终返回通过、答案错误、运行错误、编译错误、超时、超内存等结果[1-4]。

典型在线判题系统通常包含题库管理、测试用例管理、代码编辑、提交记录、评测队列、执行环境、结果回传和统计分析等模块。题库管理负责题目内容、难度、标签和测试用例维护；判题模块负责根据语言编译或解释执行用户程序；提交记录模块负责持久化每次评测结果；统计模块用于展示用户训练情况。

OJPT当前不追求大型竞赛平台的复杂竞赛组织能力，而是把重点放在个人训练和题库维护上。系统需要保证普通用户能够完成完整做题链路，管理员能够维护题目和测试数据，判题结果能够被记录和查询。该范围既符合本科毕业设计的可实现性，也能够体现在线判题系统的核心业务特征。

## 2.2 SpringBoot与后端分层开发

SpringBoot是基于Spring生态的快速开发框架，能够通过自动配置、依赖管理和内嵌服务器降低Java Web应用搭建成本。OJPT后端使用SpringBoot组织REST接口、业务服务、数据访问、安全认证和测试代码[16]。

系统后端采用常见分层结构。Controller层负责接收HTTP请求、参数校验和统一响应；Service层负责业务规则、状态流转和事务处理；Mapper层负责数据库读写；Entity、DTO和VO分别用于数据库实体、请求参数和响应对象表达。分层设计能够降低模块耦合，使认证、题库、判题、用户中心和管理端功能具有较清晰的职责边界[5-7][17][18]。

SpringBoot还为系统提供Actuator健康检查、测试支持和配置管理能力。结合springdoc-openapi，系统能够暴露Swagger UI形式的接口文档，便于接口测试和论文截图取证。

## 2.3 Vue 3与前端工程化

Vue 3是渐进式前端框架，适合构建单页应用。OJPT前端使用Vue 3、TypeScript、Vue Router、Pinia和Element Plus实现页面和交互。Vue Router用于组织首页、题库、做题页、个人中心和管理后台等路由；Pinia用于维护用户信息、访问令牌和登录状态；Element Plus提供表格、表单、弹窗、按钮、分页和消息提示等常用组件。

TypeScript为前端接口数据和组件状态提供类型约束，能够减少字段拼写错误和接口结构不一致问题。Axios请求层统一注入访问令牌，并对接口响应、错误状态和刷新令牌逻辑进行封装。前端工程化还包括Vite构建、Vitest单元测试和Playwright端到端测试，使页面功能能够在迭代中持续验证。

OJPT前端页面按照业务角色划分。普通用户侧包含首页、题库列表、做题页面、个人中心、训练看板和提交记录；管理员侧包含管理概览、用户管理、题目管理、题目编辑和标签管理。路由守卫根据登录状态和角色信息控制页面访问，避免普通用户进入管理员页面。

## 2.4 MyBatis-Plus、MySQL与Redis

MyBatis-Plus是MyBatis的增强工具，能够简化常见CRUD、条件查询和分页查询。OJPT使用MyBatis-Plus访问MySQL数据库，并通过分页插件保证题库列表、用户列表、题目列表等接口返回正确的`total`、`pages`、`size`等分页字段。

MySQL是系统主要关系型数据库，保存用户、用户资料、密码重置申请、题目、标签、题目标签关系、测试用例、代码草稿、提交记录、提交用例结果和用户题目进度等核心数据。关系数据库适合表达题目与标签、用户与提交、题目与测试用例等结构化关系，也便于通过索引提升查询效率。

Redis在系统中用于辅助登录态管理。系统通过访问令牌和刷新令牌维护用户会话，并在用户退出登录或刷新令牌时管理令牌状态。Redis的键值存储和过期时间能力适合保存刷新令牌、访问令牌黑名单等临时状态。

## 2.5 JWT鉴权与RBAC权限控制

JWT（JSON Web Token）是一种常用于前后端分离系统的认证令牌格式。用户登录成功后，后端生成访问令牌和刷新令牌，前端在后续请求中通过`Authorization: Bearer <token>`携带访问令牌。后端过滤器解析令牌后获取用户身份和角色信息，再决定请求是否允许继续执行[17][20]。

OJPT结合JWT和RBAC（Role-Based Access Control，基于角色的访问控制）实现权限隔离。系统当前主线角色包括普通用户`USER`和管理员`ADMIN`。普通用户可以访问题库、做题、个人中心、训练看板和提交记录；管理员可以访问后台用户管理、题目管理、标签管理、统计概览和判题环境健康检查等功能。

权限控制需要同时考虑匿名访问和敏感操作。题库列表和已发布题目详情允许匿名访问，便于用户先浏览题目；代码草稿、运行代码、正式提交、个人中心和训练数据需要登录后访问；管理端接口必须具有管理员角色。该策略保证公开内容易访问，同时避免敏感数据和维护操作被未授权用户使用。

## 2.6 Docker判题环境

在线判题系统需要执行用户提交的代码。用户代码具有不确定性，可能出现死循环、异常退出或恶意访问系统资源等情况，因此不能直接在业务服务进程中运行。Docker容器能够为不同语言提供相对隔离的运行环境，并通过资源限制控制运行时间、内存和文件系统访问范围。

OJPT判题模块支持C/C++、Java和Python3等语言。系统根据用户选择的语言准备源代码文件、输入数据和资源限制，再调用Docker环境执行程序。运行结果包括标准输出、错误输出、耗时、内存和状态信息。正式提交时，系统会遍历题目的测试用例，将每个用例的运行结果保存为提交用例结果，并根据整体结果更新提交状态和用户题目进度。

管理员后台提供判题环境健康检查功能，用于检查Docker可执行文件、版本信息和语言镜像是否可用。该功能能够在系统运行前发现环境依赖问题，降低判题失败的排查成本。

## 2.7 Markdown渲染与内容安全

算法题面通常包含标题、段落、列表、代码块、输入输出格式和样例说明。Markdown语法轻量、可读性好，适合存储和编辑题目描述。OJPT使用Markdown存储题面内容，并在普通用户题目详情和管理员编辑预览中渲染。

由于Markdown最终会被转换成HTML，渲染过程需要防止脚本注入和危险属性。系统前端在解析Markdown后进行DOM清洗，移除潜在风险标签和属性，再通过统一样式展示标题、段落、列表、表格和代码块。这样既保证题面表达能力，也降低XSS风险。

管理员编辑题目时，系统提供Markdown编辑与实时预览。管理员可以在保存或发布前检查题面显示效果，减少格式错误对普通用户阅读体验的影响。

## 2.8自动化测试技术

自动化测试是保证系统迭代稳定性的重要手段。OJPT后端使用JUnit和SpringBoot Test编写服务层、控制器和配置相关测试，覆盖用户注册、密码重置、题库服务、代码草稿、运行代码、提交创建、判题环境健康检查和权限控制等逻辑[9-12][19][20]。

前端使用Vitest和Vue Test Utils编写单元测试，覆盖认证请求、题库接口、提交接口、用户接口、管理员接口、路由守卫、登录状态存储、题库筛选、做题页交互、个人中心布局和管理端组件等内容。端到端测试使用Playwright覆盖登录、权限跳转、题库分页、题号路由、管理员题目流程和管理看板等用户可见链路。

通过后端测试、前端单元测试、端到端测试和构建检查组合，系统能够对服务逻辑、页面交互和关键流程进行较全面验证。

<!-- pagebreak -->

# 3系统需求分析

## 3.1系统功能需求分析

OJPT在线编程训练平台面向个人算法训练和题库维护场景，系统功能围绕“选择题目、编辑代码、运行调试、提交判题、查看训练数据、维护题库资源”展开。普通用户侧重点在训练闭环，管理员侧重点在基础数据和运行环境维护。需求分析不再按用户角色单独铺开，而是直接按照系统功能组织说明，使需求描述与后续系统实现章节保持一致。

### 3.1.1系统主要功能介绍

系统的账号认证功能负责登录、注册、令牌刷新、退出登录、当前用户信息查询和密码重置申请。用户可以使用用户名、邮箱或手机号登录，注册成功后系统返回登录态；访问个人中心、代码草稿、运行代码、正式提交和后台管理等受保护功能时，系统根据JWT和角色权限进行校验。

题库与在线做题功能负责题目列表展示、题目筛选、题目详情加载、Markdown题面展示、公开样例加载、代码编辑和语言切换。用户可以按题号、关键词、难度、标签和完成状态查找题目，进入做题页后可以阅读题面、查看限制条件和样例数据，并在代码编辑区完成作答。

代码运行与提交判题功能负责保存代码草稿、运行样例或自定义测试用例、正式提交代码、查询提交结果和更新训练进度。代码运行主要服务于提交前调试，不创建正式提交记录；提交判题会创建提交记录，调用Docker判题环境执行测试用例，保存逐用例结果，并更新用户题目进度。

个人中心与训练数据功能负责个人资料维护、头像上传与删除、账号安全设置、提交记录查看和训练看板展示。训练看板需要展示提交次数、通过次数、已解决题数、通过率、难度分布、状态分布和近期提交记录，使用户能够了解自己的训练情况。

后台管理功能负责用户管理、题目管理、测试用例维护、标签管理、密码重置审核、统计概览和判题环境检测。管理员可以维护用户状态和资料，创建题目草稿，编辑题面和限制条件，维护公开样例和隐藏用例，绑定题目标签，并检查Docker可执行文件、版本和语言镜像状态。

### 3.1.2系统功能模块分析

根据系统功能范围，OJPT可以划分为认证与账号、题库浏览、在线做题、代码运行、提交判题、训练数据、后台用户管理、后台题目管理、后台标签管理和判题环境检测等模块。各模块之间以题库和判题为核心形成联系：题库模块提供题目数据，在线做题模块组织题面和代码编辑，代码运行与提交判题模块产生运行结果和提交记录，训练数据模块读取提交记录与题目进度，后台管理模块维护题目、测试用例、标签和用户状态。系统功能模块如图3-1所示。

![图3-1系统功能模块图](../功能说明书/assets/figures/paper/fig-3-1-function-modules.png)

图3-1系统功能模块图

### 3.1.3系统用例分析

系统用例主要包括普通用户训练用例、管理员维护用例和判题环境交互用例。普通用户可以完成登录注册、浏览题库、在线做题、运行代码、提交判题和查看训练数据；管理员在具备普通用户能力的基础上，可以完成用户管理、题目管理、测试用例维护、标签管理、统计概览和判题环境检测；判题环境作为外部执行环境，参与代码运行、正式判题和环境检测。系统用例图如图3-2所示。

![图3-2系统用例图](../功能说明书/assets/figures/paper/fig-3-2-use-case.png)

图3-2系统用例图

## 3.2系统功能流程介绍

OJPT在线编程训练平台的功能流程可以通过数据流图进行描述。普通用户、管理员、Docker判题环境和数据库是系统数据交换的主要参与方，题库浏览、在线做题、代码运行、提交判题、训练统计和后台维护等处理过程共同构成系统的数据处理链路。

### 3.2.1系统数据流分析

顶层数据流图用于描述系统与外部实体之间的主要数据交换关系。普通用户向系统提交登录信息、题库筛选条件、源代码、测试输入和个人资料维护请求，系统向普通用户返回题目列表、题目详情、运行结果、提交结果和训练数据。管理员向系统提交用户、题目、测试用例、标签和判题环境检查等维护请求，系统返回管理结果、统计数据和环境状态。Docker判题环境接收源码、语言、输入数据和资源限制，返回执行结果。系统顶层数据流图如图3-3所示。

![图3-3系统顶层数据流图](../功能说明书/assets/figures/paper/fig-3-2-top-dfd.png)

图3-3系统顶层数据流图

零层数据流图在顶层数据流图的基础上，将系统内部处理过程拆分为用户认证、题库浏览、在线做题、运行判题、训练数据和后台维护等部分。各处理过程通过用户信息、题目信息、测试用例、代码草稿、提交记录、提交用例结果和用户题目进度等数据存储进行联系，能够反映系统内部主要数据的产生、读取和更新关系。系统零层数据流图如图3-4所示。

![图3-4系统零层数据流图](../功能说明书/assets/figures/paper/fig-3-3-zero-dfd.png)

图3-4系统零层数据流图

一层数据流图重点展开代码运行和提交判题这一核心业务。用户进入做题页后，系统读取题目详情、公开样例和代码草稿；用户运行代码时，系统将源码、语言和输入数据发送给Docker判题环境，并返回标准输出、错误输出、耗时和状态；用户正式提交时，系统创建提交记录，读取题目测试用例，保存逐用例结果，并更新用户题目进度。一层数据流图如图3-5所示。

![图3-5一层数据流图](../功能说明书/assets/figures/paper/fig-3-4-level1-judge-dfd.png)

图3-5一层数据流图

### 3.2.2数据字典

数据字典中包含数据源、数据流、数据存储和数据加工等内容，通过数据字典可以明确系统与用户、管理员、判题环境和数据库之间的数据走向及处理情况。

数据源

表3-1数据源表

| 序号 | 名称 | 简述 | 输入/输出 |
| --- | --- | --- | --- |
| 1 | 普通用户 | 浏览题库、在线做题、运行代码、提交判题和查看训练数据 | 输入登录信息、筛选条件、源码和测试输入；接收题目、运行结果、提交结果和训练数据 |
| 2 | 管理员 | 维护用户、题目、测试用例、标签和判题环境 | 输入管理操作和维护数据；接收统计数据、维护结果和环境检测结果 |
| 3 | Docker判题环境 | 隔离执行用户代码并返回运行状态 | 接收源码、语言、输入数据和资源限制；输出标准输出、错误输出、耗时和状态 |
| 4 | MySQL/Redis | 保存业务数据和登录状态数据 | 接收持久化数据、令牌状态和缓存数据；输出查询结果和状态校验结果 |

数据流

表3-2数据流表

| 序号 | 名称 | 来源 | 去向 | 组成 |
| --- | --- | --- | --- | --- |
| 1 | 登录信息 | 用户 | 用户认证处理 | 账号、密码、验证码状态 |
| 2 | 用户身份信息 | 用户认证处理 | 前端与权限控制 | 用户ID、用户名、角色、访问令牌、刷新令牌 |
| 3 | 题库查询条件 | 用户 | 题库浏览处理 | 关键词、题号、难度、标签、完成状态、分页参数 |
| 4 | 题目详情数据 | 题库浏览处理 | 在线做题处理 | 题号、标题、题面、限制条件、样例、标签 |
| 5 | 运行请求数据 | 在线做题处理 | 代码运行处理 | 语言、源码、测试输入、期望输出 |
| 6 | 提交请求数据 | 在线做题处理 | 提交判题处理 | 题号、语言、源码、用户信息 |
| 7 | 判题结果数据 | Docker判题环境 | 提交记录与结果展示 | 状态、输出、错误信息、耗时、内存 |
| 8 | 管理维护数据 | 管理员 | 后台管理处理 | 用户信息、题目信息、测试用例、标签和发布状态 |

数据存储

表3-3数据存储表

| 序号 | 名称 | 编号 | 组成 | 相关处理 |
| --- | --- | --- | --- | --- |
| 1 | 用户信息表 | D1 | 用户ID、用户名、密码、邮箱、手机号、角色、状态 | 登录注册、权限判断、用户管理 |
| 2 | 题目信息表 | D2 | 题目ID、题号、标题、难度、题面、限制、状态 | 题库查询、题目详情、题目维护 |
| 3 | 测试用例表 | D3 | 用例ID、题目ID、输入、期望输出、类型、排序 | 代码运行、提交判题、用例维护 |
| 4 | 代码草稿表 | D4 | 用户ID、题目ID、语言、源码、更新时间 | 草稿保存、做题页初始化 |
| 5 | 提交记录表 | D5 | 提交ID、用户ID、题目ID、语言、源码、状态、耗时 | 正式提交、提交记录查询 |
| 6 | 提交用例结果表 | D6 | 提交ID、用例序号、状态、输入、期望输出、实际输出 | 提交详情、结果展示 |
| 7 | 用户题目进度表 | D7 | 用户ID、题目ID、进度状态、最近提交ID | 训练看板、题库完成状态 |
| 8 | 标签与题目标签表 | D8 | 标签ID、标签名、题目ID、标签ID | 题库筛选、题目标签维护 |

数据加工

表3-4数据加工表

| 序号 | 名称 | 输入 | 输出 | 处理逻辑 |
| --- | --- | --- | --- | --- |
| 1 | 用户认证处理 | 登录信息、注册信息 | 用户身份信息、令牌状态 | 校验账号、密码和用户状态，生成或刷新访问令牌 |
| 2 | 题库浏览处理 | 题库查询条件 | 题目列表、分页数据 | 按关键词、题号、难度、标签和完成状态组合查询 |
| 3 | 做题页初始化处理 | 题号、用户、语言 | 题目详情、公开样例、代码草稿 | 查询题面、样例和当前用户草稿 |
| 4 | 代码运行处理 | 源码、语言、测试输入 | 运行输出、错误信息、耗时、状态 | 调用Docker判题环境执行临时测试，不生成正式提交记录 |
| 5 | 提交判题处理 | 题号、源码、语言、用户信息 | 提交记录、逐用例结果、题目进度 | 创建提交记录，执行全部测试用例，保存结果并更新训练进度 |
| 6 | 训练数据处理 | 提交记录、题目进度 | 提交统计、通过率、难度分布 | 汇总用户提交与通过情况，生成训练看板数据 |
| 7 | 后台维护处理 | 管理维护数据 | 维护结果、统计数据、环境状态 | 维护用户、题目、测试用例、标签和判题环境信息 |

<!-- pagebreak -->

# 4系统分析与设计

## 4.1系统总体架构设计

OJPT采用前后端分离架构，整体由浏览器前端、后端服务、关系数据库、缓存服务和Docker判题环境组成。前端层负责页面展示、路由跳转、登录状态维护、接口请求封装和用户交互，普通用户通过前端访问题库、做题页和个人中心，管理员通过前端访问管理端。后端服务层负责业务处理和接口响应，覆盖认证、题库、做题、用户中心和管理端等业务。数据层由MySQL和Redis构成，MySQL保存用户、题目、标签、测试用例、代码编辑内容、提交记录和用户进度等核心业务数据，Redis用于保存刷新令牌和访问令牌黑名单等临时认证状态。Docker判题环境独立执行用户代码，后端根据执行结果更新提交记录和用户进度。该架构能够将页面展示、业务处理、数据存储和判题执行解耦，便于系统分别进行开发、调试、维护和后续扩展。

## 4.2系统部署结构设计

本地开发和演示环境中，前端Vite服务运行在`8110`端口，后端SpringBoot服务运行在`8111`端口，MySQL、Redis和Docker作为后端依赖服务运行。用户通过浏览器访问前端页面，前端通过HTTP API调用后端接口。部署设计要求前端服务和后端服务解耦，便于分别启动、构建和调试；后端统一连接MySQL和Redis，数据库结构由Flyway migration管理；Docker判题环境由后端调度，不直接暴露给普通用户；管理端可以检查Docker可执行文件、版本和镜像状态；上传头像等静态资源需要有稳定的服务端存储路径和访问路径。系统部署结构如图4-2所示。

![图4-2系统部署结构图](../功能说明书/assets/figures/paper/fig-4-2-deployment.png)

图4-2系统部署结构图

## 4.3数据库概念结构设计

OJPT数据库概念结构围绕用户、题目、标签、测试用例、代码草稿、提交记录、提交用例结果和用户题目进度展开。实体说明采用段落编号形式，不再作为三级标题进入目录。

（1）用户实体用于保存登录账号、联系方式、头像、状态和角色等信息。用户扩展资料通过`user_profile`表维护，密码重置申请通过`password_reset_request`表维护。用户实体属性图如图4-3所示。

![图4-3用户实体属性图](../功能说明书/assets/figures/paper/fig-4-3-user-entity.png)

图4-3用户实体属性图

（2）题目实体保存题号、标题、难度、题面Markdown、时间限制、内存限制、状态、提交次数和通过次数等信息。题目状态包括`DRAFT`、`PUBLISHED`和`ARCHIVED`。题目实体属性图如图4-4所示。

![图4-4题目实体属性图](../功能说明书/assets/figures/paper/fig-4-4-problem-entity.png)

图4-4题目实体属性图

（3）标签实体用于对题目进行分类和筛选。题目和标签之间通过`problem_tag`关系表建立多对多关系。标签实体属性图如图4-5所示。

![图4-5标签实体属性图](../功能说明书/assets/figures/paper/fig-4-5-tag-entity.png)

图4-5标签实体属性图

（4）测试用例实体保存题目输入、期望输出、用例类型、解释说明和排序信息。用例类型包括公开样例`SAMPLE`和隐藏用例`HIDDEN`。测试用例实体属性图如图4-6所示。

![图4-6测试用例实体属性图](../功能说明书/assets/figures/paper/fig-4-6-testcase-entity.png)

图4-6测试用例实体属性图

（5）代码草稿实体保存用户在某道题、某种语言下的源代码，并通过用户、题目和语言三个维度确定唯一记录。代码草稿实体属性图如图4-7所示。

![图4-7代码草稿实体属性图](../功能说明书/assets/figures/paper/fig-4-7-code-draft-entity.png)

图4-7代码草稿实体属性图

（6）提交记录实体保存正式提交的源代码、语言、状态、运行耗时、内存、编译信息、判题信息和提交时间。提交记录实体属性图如图4-8所示。

![图4-8提交记录实体属性图](../功能说明书/assets/figures/paper/fig-4-8-submission-entity.png)

图4-8提交记录实体属性图

（7）提交用例结果实体保存单个测试用例的判题结果，包括用例类型、用例序号、状态、输入、期望输出、实际输出、错误输出、耗时、内存和消息。提交用例结果实体属性图如图4-9所示。

![图4-9提交用例结果实体属性图](../功能说明书/assets/figures/paper/fig-4-9-submission-case-result-entity.png)

图4-9提交用例结果实体属性图

（8）用户题目进度实体用于记录用户对某道题的当前进度。状态包括`UNSOLVED`、`ATTEMPTED`和`SOLVED`。用户题目进度实体属性图如图4-10所示。

![图4-10用户题目进度实体属性图](../功能说明书/assets/figures/paper/fig-4-10-user-problem-progress-entity.png)

图4-10用户题目进度实体属性图

用户与提交记录、题目与提交记录、题目与测试用例、题目与标签、提交记录与提交用例结果、用户与题目进度之间形成核心关系。整体关系以题目为中心，一道题可以有多个测试用例和多个标签；一个用户可以对多道题保存草稿、产生提交记录并形成题目进度；一次提交可以产生多条提交用例结果。数据库整体E-R图如图4-11所示。

![图4-11数据库整体E-R图](../功能说明书/assets/figures/paper/fig-4-11-er-overview.png)

图4-11数据库整体E-R图

## 4.4概念模型向关系模型的转换

完成数据库概念结构设计后，需要将E-R图中的实体、属性和联系转化为关系数据库中的表、字段和约束。OJPT采用MySQL保存主要业务数据，因此关系模型设计时将实体映射为数据表，将实体属性映射为表字段，将实体标识映射为主键，将实体之间的联系映射为外键字段或中间关系表。

根据前文数据库整体E-R图，系统主要概念模型元素与关系模型的转换结果如表4-1所示。

表4-1概念模型向关系模型转换表

| 概念模型元素 | 关系模型结果 | 主要键或联系 | 转换说明 |
| --- | --- | --- | --- |
| 用户实体 | `user`、`user_profile` | `user.id`，`user_profile.user_id` | 账号基础信息和扩展资料分表保存，用户ID作为关联依据 |
| 题目实体 | `problem` | `problem.id`，`problem.problem_no` | 题目ID作为主键，题号作为业务唯一标识 |
| 标签实体 | `tag` | `tag.id` | 保存标签名称和类型，供题库筛选使用 |
| 题目与标签联系 | `problem_tag` | `problem_id`、`tag_id` | 多对多联系转化为中间表 |
| 测试用例实体 | `problem_test_case` | `problem_test_case.id`，`problem_id` | 题目与测试用例的一对多联系通过题目ID保存 |
| 代码草稿实体 | `problem_code_draft` | `user_id`、`problem_id`、`language` | 用户、题目和语言共同确定一份草稿 |
| 提交记录实体 | `submission` | `submission.id`，`user_id`，`problem_id` | 用户与题目产生提交记录，保存源码和判题状态 |
| 提交用例结果实体 | `submission_case_result` | `submission_id`、`case_index` | 一次提交可产生多条测试用例执行结果 |
| 用户题目进度联系 | `user_problem_progress` | `user_id`、`problem_id` | 记录用户对题目的完成状态和最近一次提交 |

实体之间的一对多联系主要通过外键字段表示。例如，题目与测试用例之间是一对多关系，`problem_test_case`表通过`problem_id`关联`problem`表；用户与提交记录之间是一对多关系，`submission`表通过`user_id`关联`user`表；提交记录与提交用例结果之间是一对多关系，`submission_case_result`表通过`submission_id`关联`submission`表。

实体之间的多对多联系通过中间表表示。题目和标签之间不存在直接字段冗余，而是通过`problem_tag`表保存`problem_id`和`tag_id`，从而支持一个题目绑定多个标签、一个标签对应多个题目。用户与题目之间的训练状态虽然也具有多对多特点，但系统需要保存完成状态、最近提交等附加信息，因此单独设计`user_problem_progress`表表达该关系。经过上述转换后，关系模型能够支撑登录认证、题库维护、在线做题、提交判题、训练统计和后台管理等核心业务。

## 4.5数据库表的设计

系统数据库表设计遵循主键唯一、外键关系清晰、状态字段枚举化和常用查询字段建索引的原则。论文主线涉及用户信息表、题目信息表、标签表、题目标签关系表、测试用例表、代码草稿表、提交记录表、提交用例结果表和用户题目进度表等核心表。各表字段与系统实体、接口返回和页面展示保持对应关系，能够支撑用户训练流程和管理员维护流程。

表4-2用户信息表`user`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 用户ID，主键 |
| `username` | varchar | 用户名 |
| `password` | varchar | 加密后的密码 |
| `email` | varchar | 邮箱 |
| `phone` | varchar | 手机号 |
| `avatar` | varchar | 头像路径 |
| `status` | tinyint | 用户状态 |
| `role_type` | enum | 角色类型，`USER`或`ADMIN` |
| `created_at`、`updated_at` | datetime | 创建和更新时间 |

表4-3题目信息表`problem`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 题目ID，主键 |
| `problem_no` | varchar | 题号，唯一 |
| `title` | varchar | 题目标题 |
| `difficulty` | enum | 难度，`EASY`、`MEDIUM`、`HARD` |
| `statement_md` | text | Markdown题面 |
| `time_limit_ms` | int | 时间限制 |
| `memory_limit_kb` | int | 内存限制 |
| `status` | enum | 题目状态 |
| `submit_count`、`accepted_count` | int | 提交次数和通过次数 |

表4-4标签和题目标签关系表

| 表名 | 关键字段 | 说明 |
| --- | --- | --- |
| `tag` | `id`、`name`、`type`、`created_at`、`updated_at` | 保存标签名称和类型 |
| `problem_tag` | `id`、`problem_id`、`tag_id`、`created_at` | 保存题目与标签的多对多关系 |

表4-5测试用例表`problem_test_case`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 测试用例ID |
| `problem_id` | bigint | 所属题目ID |
| `case_type` | enum | `SAMPLE`或`HIDDEN` |
| `input_text` | text | 输入数据 |
| `expected_output` | text | 期望输出 |
| `explanation` | text | 用例说明 |
| `sort_order` | int | 排序号 |

表4-6代码编辑内容表`problem_code_draft`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 草稿ID |
| `user_id` | bigint | 用户ID |
| `problem_id` | bigint | 题目ID |
| `language` | varchar | 编程语言 |
| `source_code` | text | 源代码 |
| `created_at`、`updated_at` | datetime | 创建和更新时间 |

表4-7提交记录表`submission`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | bigint | 提交ID |
| `user_id` | bigint | 用户ID |
| `problem_id` | bigint | 题目ID |
| `language` | varchar | 编程语言 |
| `source_code` | text | 提交源码 |
| `status` | enum | 判题状态 |
| `time_ms` | int | 运行耗时 |
| `memory_kb` | int | 内存使用 |
| `compile_message`、`judge_message` | text | 编译信息和判题信息 |

表4-8提交用例结果和用户题目进度表

| 表名 | 关键字段 | 说明 |
| --- | --- | --- |
| `submission_case_result` | `submission_id`、`case_type`、`case_index`、`status`、`actual_output`、`time_ms` | 保存单个测试用例执行结果 |
| `user_problem_progress` | `user_id`、`problem_id`、`status`、`last_submission_id`、`updated_at` | 保存用户对题目的最新进度 |

<!-- pagebreak -->

# 5系统的开发设计与实现

## 5.1用户登录功能的设计与实现

### 5.1.1功能设计

用户进入系统后，可以通过登录弹窗输入账号和密码。账号支持用户名、邮箱和手机号三种形式，前端提交登录信息后，由后端认证接口统一处理。后端先根据账号类型定位用户，再交给Spring Security完成密码校验。校验通过后，系统读取用户状态和角色信息，生成访问令牌和刷新令牌，并把令牌、用户基本信息和角色列表返回给前端。

前端收到登录结果后，将访问令牌保存到用户状态中，后续访问个人中心、代码草稿、提交判题和后台管理等受保护接口时，在请求头中携带该令牌。若账号不存在、密码错误、账号被禁用或令牌状态异常，系统返回失败提示，用户需要重新输入登录信息。用户登录流程如图5-1所示。

![图5-1用户登录流程图](../功能说明书/assets/figures/paper/fig-5-1-login-flow.png)

图5-1用户登录流程图

该功能的核心代码：

```java
@PostMapping("/login")
public Result<LoginResponseVO> login(@Valid @RequestBody LoginRequestDTO dto) {
    String principalName = resolvePrincipalName(dto.getAccount());
    Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(principalName, dto.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    LoginUserDetails principal = (LoginUserDetails) authentication.getPrincipal();
    List<String> roles = principal.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .toList();
    JwtService.TokenPair pair = jwtService.generateTokens(
            principal.getUserId(), principal.getUsername(), roles);
    refreshTokenStore.store(principal.getUserId(), pair.refreshJti(), pair.refreshToken());
    User dbUser = userService.findById(principal.getUserId());
    return Result.ok(authConverter.toLoginResponse("Bearer", pair,
            jwtProperties.getAccessExpSeconds(), jwtProperties.getRefreshExpSeconds(), dbUser, roles));
}
```


### 5.1.2界面设计

登录界面采用弹窗形式展示，用户可以输入用户名、邮箱或手机号以及密码完成身份认证。界面在提交失败时显示错误提示，登录成功后顶部导航切换为用户菜单。

![图5-2 用户登录界面](../功能说明书/assets/screenshots/auth/auth-01-login-dialog.png)

图5-2 用户登录界面

## 5.2用户注册功能的设计与实现

### 5.2.1功能设计

用户未拥有账号时，可以在注册弹窗中填写用户名、密码、邮箱或手机号等信息。前端先进行基础表单校验，校验通过后调用注册接口。后端接收注册请求后，对用户名、邮箱、手机号进行唯一性检查，并对密码进行加密存储，防止明文密码进入数据库。

注册成功后，系统直接为新用户分配普通用户角色，生成访问令牌和刷新令牌，使用户无需再次回到登录页即可进入训练流程。若用户名、邮箱或手机号已被占用，后端返回明确错误信息，前端提示用户重新填写。用户注册流程如图5-3所示。

![图5-3用户注册流程图](../功能说明书/assets/figures/paper/fig-5-7-register-flow.png)

图5-3用户注册流程图

该功能的核心代码：

```java
@PostMapping("/register")
public Result<LoginResponseVO> register(@Valid @RequestBody RegisterRequestDTO dto) {
    User user = userService.register(dto);
    List<String> roles = List.of("USER");
    JwtService.TokenPair pair = jwtService.generateTokens(user.getId(), user.getUsername(), roles);
    refreshTokenStore.store(user.getId(), pair.refreshJti(), pair.refreshToken());
    LoginResponseVO vo = authConverter.toLoginResponse(
            "Bearer", pair, jwtProperties.getAccessExpSeconds(),
            jwtProperties.getRefreshExpSeconds(), user, roles);
    return Result.ok(vo);
}

public User register(RegisterRequestDTO dto) {
    String account = dto.getAccount().trim();
    String password = dto.getPassword();
    String nickname = dto.getNickname().trim();
    boolean emailAccount = account.contains("@");
    String email = emailAccount ? account.toLowerCase() : null;
    String phone = emailAccount ? null : account;
    if (email != null && findByEmail(email) != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已被使用");
    }
    if (phone != null && findByPhone(phone) != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "手机号已被使用");
    }
    User user = new User()
            .setUsername(buildUniqueUsername(nickname, account))
            .setPassword(passwordEncoder.encode(password))
            .setEmail(email)
            .setPhone(phone)
            .setRoleType("USER")
            .setStatus(1);
    userMapper.insert(user);
    return user;
}
```


### 5.2.2界面设计

注册界面与登录入口保持一致，用户填写账号、密码和基础资料后提交注册请求。表单会对必填项和账号格式进行提示，注册成功后系统自动进入登录态。

![图5-4 用户注册界面](../功能说明书/assets/screenshots/auth/auth-02-register-form.png)

图5-4 用户注册界面

## 5.3题库浏览功能的设计与实现

### 5.3.1功能设计

题库浏览功能面向匿名用户和已登录用户开放。用户进入题库页面后，可以按页码、关键词、难度、标签、完成状态和排序条件查询题目。前端把筛选条件组织为查询参数，请求后端题库分页接口，后端返回题号、标题、难度、标签、提交次数和用户完成状态等信息。

后端查询题库时，会结合题目发布状态和用户身份进行处理。匿名用户只能看到已发布题目的公开信息；已登录用户还会结合用户题目进度表，返回未开始、已尝试或已解决等训练状态。题库浏览流程如图5-5所示。

![图5-5题库浏览流程图](../功能说明书/assets/figures/paper/fig-5-2-problem-filter-flow.png)

图5-5题库浏览流程图

该功能的核心代码：

```java
@GetMapping
public Result<PageResult<ProblemListItemVO>> listProblems(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "20") Integer size,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "difficulty", required = false) String difficulty,
        @RequestParam(value = "tagId", required = false) Long tagId,
        @RequestParam(value = "status", required = false) String status,
        @RequestParam(value = "orderBy", required = false) String orderBy) {
    PageResult<ProblemListItemVO> pageResult = problemService.queryProblems(
            getCurrentUserId(), page, size, keyword, difficulty, tagId, status, orderBy);
    return Result.ok(pageResult);
}
```


### 5.3.2界面设计

题库浏览界面以列表形式展示题号、标题、难度、标签和完成状态。用户可以通过关键词、题号、难度、标签和状态组合筛选题目，并通过分页查看更多题目。

![图5-6 题库浏览界面](../功能说明书/assets/screenshots/user/user-02-problemset.png)

图5-6 题库浏览界面

## 5.4在线做题功能的设计与实现

### 5.4.1功能设计

用户从题库列表点击题目后进入做题页面。页面初始化时，前端根据题号请求题目详情，加载题面、输入输出说明、时间限制、内存限制、难度、标签和提交统计等内容。题面采用Markdown存储，前端渲染前会进行安全清洗，避免危险HTML影响页面安全。

做题页还会加载公开样例测试用例，并根据当前用户、题目和语言读取代码草稿。若用户未登录或没有历史草稿，页面使用默认代码模板；若存在草稿，则恢复上次编辑内容。在线做题流程如图5-7所示。

![图5-7在线做题流程图](../功能说明书/assets/figures/paper/fig-5-20-problem-detail-init-flow.png)

图5-7在线做题流程图

该功能的核心代码：

```java
@GetMapping("/no/{problemNo}")
public Result<ProblemDetailVO> getProblemDetailByNo(@PathVariable("problemNo") Integer problemNo) {
    return Result.ok(problemService.getProblemDetailByNo(problemNo, getCurrentUserId()));
}

@GetMapping("/no/{problemNo}/test-cases/sample")
public Result<List<ProblemTestCaseVO>> getProblemSampleTestCases(
        @PathVariable("problemNo") Integer problemNo) {
    return Result.ok(problemTestCaseService.getSampleTestCasesByProblemNo(problemNo));
}
```


### 5.4.2界面设计

在线做题界面分为题面展示、代码编辑、测试用例和运行结果区域。用户可以阅读Markdown题面，选择语言并在编辑器中编写代码。

![图5-8 在线做题界面](../功能说明书/assets/screenshots/judge/judge-02-problem-statement.png)

图5-8 在线做题界面

## 5.5代码草稿功能的设计与实现

### 5.5.1功能设计

代码草稿功能用于保存用户在做题页中的临时代码。用户编辑代码后，前端会在手动保存或自动保存触发时，把题号、语言和源码提交给后端。后端根据用户ID、题目ID和语言确定唯一草稿记录，若记录不存在则新增，若记录已存在则更新源码和更新时间。

草稿保存后，用户重新进入同一道题或切换回相同语言时，可以恢复此前编辑内容，减少页面刷新、误关闭或切换语言造成的代码丢失。代码草稿保存流程如图5-9所示。

![图5-9代码草稿流程图](../功能说明书/assets/figures/paper/fig-5-3-code-draft-flow.png)

图5-9代码草稿流程图

该功能的核心代码：

```java
public ProblemCodeDraftVO saveDraft(Long userId, Integer problemNo, ProblemCodeDraftSaveDTO dto) {
    Problem problem = requireProblem(problemNo);
    String language = dto.getLanguage().trim();
    ProblemCodeDraft draft = findDraft(userId, problem.getId(), language);
    if (draft == null) {
        draft = new ProblemCodeDraft()
                .setUserId(userId)
                .setProblemId(problem.getId())
                .setLanguage(language)
                .setSourceCode(dto.getSourceCode());
        problemCodeDraftMapper.insert(draft);
    } else {
        draft.setSourceCode(dto.getSourceCode());
        problemCodeDraftMapper.updateById(draft);
    }
    return toVO(problem, draft);
}
```


### 5.5.2界面设计

代码草稿保存后，界面会给出保存成功提示。用户重新进入题目或切换回相同语言时，可以恢复上次编辑内容。

![图5-10 代码草稿保存界面](../功能说明书/assets/screenshots/judge/judge-05-draft-saved.png)

图5-10 代码草稿保存界面

## 5.6代码运行功能的设计与实现

### 5.6.1功能设计

代码运行功能用于用户正式提交前的调试。用户选择语言、填写代码和测试输入后，点击运行按钮，前端把源码、语言、输入内容和期望输出提交到运行接口。该流程只返回单次运行结果，不创建正式提交记录。

后端收到请求后，根据语言类型准备编译或解释执行命令，并通过Docker运行用户代码。系统收集标准输出、标准错误、运行耗时和内存占用，再比较实际输出与期望输出，返回AC、WA、CE、RE、TLE等状态。代码运行流程如图5-11所示。

![图5-11代码运行流程图](../功能说明书/assets/figures/paper/fig-5-4-code-run-flow.png)

图5-11代码运行流程图

该功能的核心代码：

```java
@PostMapping("/run")
public Result<CodeRunResultVO> runCode(@Valid @RequestBody CodeRunDTO dto) {
    return Result.ok(submissionService.runCode(dto));
}

public CodeRunResultVO runCode(CodeRunDTO dto) {
    if (dto.getCases() == null || dto.getCases().isEmpty()) {
        throw BusinessException.badRequest("至少需要一个运行用例");
    }
    List<CodeRunCaseResultVO> caseResults = new ArrayList<>();
    for (int i = 0; i < dto.getCases().size(); i++) {
        CodeRunCaseDTO runCase = dto.getCases().get(i);
        CodeExecutionResult executionResult = codeExecutionService.execute(
                dto.getLanguage(), dto.getSourceCode(), runCase.getInputText(),
                dto.getTimeLimitMs(), dto.getMemoryLimitKb());
        CodeRunCaseResultVO caseResult = toRunCaseResult(i, runCase, executionResult);
        caseResults.add(caseResult);
        if (!STATUS_AC.equals(caseResult.getStatus())) {
            break;
        }
    }
    return new CodeRunResultVO("FINISHED", caseResults);
}
```


### 5.6.2界面设计

代码运行界面展示每个测试用例的运行状态、实际输出、错误信息和耗时。用户可以根据运行反馈在正式提交前调整代码。

![图5-12 代码运行结果界面](../功能说明书/assets/screenshots/judge/judge-07-run-result.png)

图5-12 代码运行结果界面

## 5.7提交判题功能的设计与实现

### 5.7.1功能设计

提交判题功能是用户完成训练的关键环节。用户确认代码后点击提交，前端把题号、语言和源码发送到正式提交接口。后端先检查登录状态和题目状态，确认题目存在且允许提交后，创建提交记录并写入初始状态。

提交创建后，系统进入判题处理流程，按题目绑定的测试用例运行用户代码，保存每个测试点的输入、期望输出、实际输出、错误信息、耗时和状态。所有测试点执行结束后，系统更新提交总状态，并同步更新用户题目进度。提交判题流程如图5-13所示。

![图5-13提交判题流程图](../功能说明书/assets/figures/paper/fig-5-5-submit-async-judge-flow.png)

图5-13提交判题流程图

该功能的核心代码：

```java
@PostMapping("/no/{problemNo}/submissions")
public Result<SubmissionCreateResultVO> submitCode(
        @PathVariable("problemNo") Integer problemNo,
        @Valid @RequestBody SubmissionCreateDTO dto) {
    Long userId = getCurrentUserId();
    if (userId == null) {
        throw BusinessException.unauthorized("未登录");
    }
    return Result.ok(submissionService.createSubmission(userId, problemNo, dto));
}

public SubmissionCreateResultVO createSubmission(Long userId, Integer problemNo, SubmissionCreateDTO dto) {
    Problem problem = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
            .eq(Problem::getProblemNo, problemNo)
            .eq(Problem::getIsDeleted, 0)
            .eq(Problem::getStatus, STATUS_PUBLISHED));
    if (problem == null) {
        throw BusinessException.notFound("题目");
    }
    Submission submission = new Submission()
            .setUserId(userId)
            .setProblemId(problem.getId())
            .setLanguage(dto.getLanguage())
            .setSourceCode(dto.getSourceCode())
            .setStatus(STATUS_QUEUED)
            .setJudgeMessage("等待判题");
    submissionMapper.insert(submission);
    updateAttemptedProgress(userId, problem.getId(), submission.getId());
    dispatchAfterCommit(() -> processQueuedSubmission(submission.getId()));
    return new SubmissionCreateResultVO(submission.getId(), STATUS_QUEUED, "已进入判题队列");
}
```


### 5.7.2界面设计

提交判题结果界面展示总判题状态、通过用例数量、耗时、内存和失败信息。判题完成后，用户能够直接查看最终结果。

![图5-14 提交判题结果界面](../功能说明书/assets/screenshots/judge/judge-09-submit-result.png)

图5-14 提交判题结果界面

## 5.8提交结果查询功能的设计与实现

### 5.8.1功能设计

用户提交代码后，前端会打开提交详情弹窗，并根据提交ID查询判题结果。若判题尚未结束，页面继续显示等待状态；若判题完成，页面展示总状态、通过用例数、运行耗时、内存占用、失败用例和错误信息。

后端查询提交结果时，会校验当前用户身份，确保普通用户只能查看自己的提交记录。系统把提交记录、逐用例结果和必要的统计信息组装为响应对象返回给前端。提交结果查询流程如图5-15所示。

![图5-15提交结果查询流程图](../功能说明书/assets/figures/paper/fig-5-23-submission-poll-flow.png)

图5-15提交结果查询流程图

该功能的核心代码：

```java
@GetMapping("/submissions/{submissionId}")
public Result<SubmissionCreateResultVO> getSubmissionResult(
        @PathVariable("submissionId") Long submissionId) {
    Long userId = getCurrentUserId();
    if (userId == null) {
        throw BusinessException.unauthorized("未登录");
    }
    return Result.ok(submissionService.getSubmissionResult(userId, submissionId));
}
```


### 5.8.2界面设计

提交记录界面按时间展示用户历史提交，包含题目、语言、状态、耗时和内存等信息，并支持查看提交源码和判题详情。

![图5-16 提交记录查询界面](../功能说明书/assets/screenshots/user/user-05-submissions.png)

图5-16 提交记录查询界面

## 5.9个人中心与训练看板功能的设计与实现

### 5.9.1功能设计

个人中心面向已登录用户，包含个人资料、账号安全、提交记录和训练看板等功能。用户进入训练看板后，前端请求当前用户的训练统计数据。后端根据提交记录、题目难度和用户题目进度统计总提交次数、通过次数、已解决题数、通过率、状态分布、难度分布和近期提交记录。

训练看板返回的数据会在前端展示为统计卡片、图表和列表，用户可以根据这些信息了解近期训练情况和题目完成情况。个人中心与训练看板流程如图5-17所示。

![图5-17个人中心与训练看板流程图](../功能说明书/assets/figures/paper/fig-5-19-training-dashboard-flow.png)

图5-17个人中心与训练看板流程图

该功能的核心代码：

```java
@GetMapping("/me/training-dashboard")
public Result<UserTrainingDashboardVO> getCurrentUserTrainingDashboard() {
    Long userId = getCurrentUserId();
    return Result.ok(trainingDashboardService.getTrainingDashboard(userId));
}

public UserTrainingDashboardVO getTrainingDashboard(Long userId) {
    List<Object> rawStatuses = submissionMapper.selectObjs(new QueryWrapper<Submission>()
            .select("status")
            .eq("user_id", userId));
    Map<String, Long> statusDistribution = buildOrderedDistribution(rawStatuses, STATUS_ORDER);
    long totalSubmissions = rawStatuses.size();
    long acceptedSubmissions = statusDistribution.getOrDefault(STATUS_ACCEPTED, 0L);
    List<UserProblemProgress> solvedProgresses = userProblemProgressMapper.selectList(
            new LambdaQueryWrapper<UserProblemProgress>()
                    .eq(UserProblemProgress::getUserId, userId)
                    .eq(UserProblemProgress::getStatus, STATUS_SOLVED));
    UserTrainingDashboardVO vo = new UserTrainingDashboardVO();
    vo.setTotalSubmissions(totalSubmissions);
    vo.setAcceptedSubmissions(acceptedSubmissions);
    vo.setSolvedProblemCount((long) solvedProgresses.size());
    vo.setAcceptanceRate(calculateRate(acceptedSubmissions, totalSubmissions));
    vo.setStatusDistribution(statusDistribution);
    return vo;
}
```


### 5.9.2界面设计

训练看板界面通过统计卡片、状态分布、难度分布和近期提交列表展示用户训练情况，帮助用户了解题目完成进度。

![图5-18 训练看板界面](../功能说明书/assets/screenshots/user/user-04-training-dashboard.png)

图5-18 训练看板界面

## 5.10后台题目管理功能的设计与实现

### 5.10.1功能设计

后台题目管理功能面向管理员开放。管理员进入题目管理页面后，可以按关键词、难度、标签、状态和排序条件查询题目列表，也可以创建题目草稿、编辑题面、维护测试用例、绑定标签、发布题目或归档题目。前端根据管理员操作调用对应接口，后端完成参数校验和数据持久化。

题目信息保存后，普通用户侧题库和做题页面会读取更新后的题目内容。测试用例维护结果直接影响代码运行和正式提交判题，因此系统在保存测试用例时会统一替换题目下的用例集合，保证判题数据与题面维护结果一致。后台题目管理流程如图5-19所示。

![图5-19后台题目管理流程图](../功能说明书/assets/figures/paper/fig-5-6-admin-problem-flow.png)

图5-19后台题目管理流程图

该功能的核心代码：

```java
@PostMapping("/problems")
public Result<ProblemSimpleVO> createProblem(@Valid @RequestBody ProblemCreateDTO dto) {
    return Result.ok(problemService.createDraft(getCurrentUserId(), dto));
}

@PutMapping("/problems/{problemId}")
public Result<Void> updateProblem(@PathVariable Long problemId,
                                  @Valid @RequestBody ProblemUpdateDTO dto) {
    problemService.updateProblem(problemId, dto);
    return Result.ok("更新成功");
}

@PutMapping("/problems/{problemId}/test-cases")
public Result<Void> replaceProblemTestCases(@PathVariable Long problemId,
        @Valid @RequestBody ProblemTestCaseBatchUpdateDTO dto) {
    problemTestCaseService.replaceProblemTestCases(problemId, dto);
    return Result.ok("测试用例保存成功");
}
```


### 5.10.2界面设计

后台题目管理界面通过表格展示题目状态、难度和操作入口，管理员可以创建草稿、编辑题目、维护测试用例、发布或归档题目。

![图5-20 后台题目管理界面](../功能说明书/assets/screenshots/admin/admin-06-problems-list.png)

图5-20 后台题目管理界面

## 5.11后台用户管理功能的设计与实现

### 5.11.1功能设计

后台用户管理功能用于管理员维护平台注册用户。管理员可以按页码、账号状态、角色和关键词查询用户列表，查看用户详情，编辑用户资料，修改用户状态，也可以处理异常账号。后端管理接口统一要求管理员角色，普通用户无法访问。

用户状态变更会影响用户后续登录和接口访问。管理员将用户禁用或恢复后，系统更新用户表中的状态字段；用户再次登录或访问受保护接口时，会按照最新状态进行校验。后台用户管理流程如图5-21所示。

![图5-21后台用户管理流程图](../功能说明书/assets/figures/paper/fig-5-32-admin-user-list-flow.png)

图5-21后台用户管理流程图

该功能的核心代码：

```java
@GetMapping("/users")
public Result<PageResult<UserDetailVO>> getUsers(
        @RequestParam(value = "page", defaultValue = "1") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "roleType", required = false) String roleType,
        @RequestParam(value = "keyword", required = false) String keyword) {
    return Result.ok(adminService.getUsers(page, size, status, roleType, keyword));
}

@PutMapping("/users/{userId}/status")
public Result<Void> updateUserStatus(@PathVariable Long userId,
                                     @RequestBody Map<String, Integer> request) {
    adminService.updateUserStatus(userId, request.get("status"));
    return Result.ok("状态更新成功");
}
```


### 5.11.2界面设计

后台用户管理界面展示用户列表、状态、角色和操作按钮，管理员可以筛选用户、查看详情、编辑资料并调整账号状态。

![图5-22 后台用户管理界面](../功能说明书/assets/screenshots/admin/admin-03-users-list.png)

图5-22 后台用户管理界面

## 5.12判题环境检测功能的设计与实现

### 5.12.1功能设计

判题环境检测功能用于管理员检查Docker和语言运行环境是否可用。管理员进入后台概览或判题环境页面后，前端请求环境健康检查接口。后端检查Docker可执行文件、Docker服务状态、语言镜像和基础运行能力，并将每项检查结果返回给前端。

若所有检查项均为正常，系统提示判题环境可用；若存在Docker不可访问、镜像缺失或语言执行失败，系统返回异常项和错误信息，方便管理员区分业务逻辑问题和基础运行环境问题。判题环境检测流程如图5-23所示。

![图5-23判题环境检测流程图](../功能说明书/assets/figures/paper/fig-5-31-admin-judge-health-flow.png)

图5-23判题环境检测流程图

该功能的核心代码：

```java
@GetMapping("/health")
public Result<JudgeEnvironmentHealthDTO> getHealth() {
    return Result.ok(judgeEnvironmentHealthService.checkHealth());
}

public JudgeEnvironmentHealthDTO checkHealth() {
    List<JudgeEnvironmentCheckDTO> checks = new ArrayList<>();
    DockerExecutableStatus executableStatus = checkDockerExecutable();
    checks.add(new JudgeEnvironmentCheckDTO("docker-executable",
            executableStatus.available() ? UP : DOWN, dockerExecutable,
            executableStatus.message()));
    if (executableStatus.available()) {
        checks.add(runCheck("docker-version", dockerExecutable,
                List.of(dockerExecutable, "version"), "Command completed successfully"));
        checks.add(runCheck("docker-info", dockerExecutable,
                List.of(dockerExecutable, "info"), "Command completed successfully"));
        checks.add(runImageCheck("cpp", cppImage));
        checks.add(runImageCheck("java", javaImage));
        checks.add(runImageCheck("python", pythonImage));
    }
    return summarize(checks);
}
```

### 5.12.2界面设计

判题环境检测界面展示Docker可执行文件、版本、服务信息和语言镜像检查结果，用于判断判题环境是否满足代码运行要求。

![图5-24 判题环境检测界面](../功能说明书/assets/screenshots/admin/admin-02-judge-health.png)

图5-24 判题环境检测界面

<!-- pagebreak -->

# 6系统测试

## 6.1测试方法

本系统采用手工测试、接口测试和自动化测试结合的方式。手工测试主要用于验证页面流程和整理运行截图，覆盖登录注册、题库筛选、在线做题、代码运行、正式提交、个人中心、训练看板和管理端维护等功能。接口测试主要验证后端API的请求路径、参数、权限和响应结构。自动化测试用于验证后端服务逻辑、前端组件交互、路由守卫和端到端业务流程。

测试过程中，先启动后端服务、前端服务、MySQL、Redis和Docker环境，再按普通用户和管理员两类角色分别执行测试。普通用户侧重点是从题库浏览到提交判题的完整训练闭环，管理员侧重点是题目、测试用例、标签、用户和判题环境的维护能力。

## 6.2测试环境

测试环境需要覆盖前端、后端、数据库、缓存和Docker判题环境。系统测试环境如表6-1所示。

表6-1测试环境表

| 项目 | 环境或工具 | 用途 |
| --- | --- | --- |
| 操作系统 | Windows开发环境 | 本地开发、运行和截图 |
| 后端运行环境 | JDK 17、SpringBoot 4 | 启动后端服务 |
| 前端运行环境 | Node.js、npm、Vite | 启动前端服务 |
| 数据库 | MySQL | 保存用户、题目、提交和测试用例数据 |
| 缓存 | Redis | 保存令牌状态 |
| 判题环境 | Docker | 隔离执行C/C++、Java、Python3代码 |
| 接口验证 | Swagger UI、浏览器开发工具 | 验证REST API响应 |
| 后端测试 | JUnit、SpringBoot Test | 验证服务层和控制器逻辑 |
| 前端测试 | Vitest、Vue Test Utils | 验证组件、状态和交互 |
| 端到端测试 | Playwright | 验证用户可见核心流程 |

系统启动后，先访问Actuator健康检查接口和Swagger UI接口文档页面，确认后端服务和接口文档可访问。健康检查截图如图6-1所示，Swagger UI截图如图6-2所示。

![图6-1 Actuator健康检查](../功能说明书/assets/screenshots/overview/overview-03-actuator-health.png)

图6-1 Actuator健康检查

![图6-2 Swagger UI接口文档](../功能说明书/assets/screenshots/overview/overview-04-swagger-ui.png)

图6-2 Swagger UI接口文档

## 6.3功能测试

功能测试按照普通用户和管理员两条主线展开。普通用户测试覆盖认证、题库浏览、题目详情、在线做题、代码运行、正式提交、训练看板和个人中心；管理员测试覆盖管理概览、用户管理、密码重置审核、题目管理、测试用例维护、标签管理和判题环境检测。

表6-2功能测试用例表

| 用例编号 | 测试模块 | 测试目的 | 操作步骤 | 期望结果 | 实际结果 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| TC-FUNC-001 | 认证与账号 | 验证正确账号登录 | 输入正确账号和密码并提交 | 登录成功，显示用户菜单 | 与期望一致 | 通过 |
| TC-FUNC-002 | 认证与账号 | 验证注册表单校验 | 输入新用户信息并提交 | 注册成功或显示明确校验提示 | 与期望一致 | 通过 |
| TC-FUNC-003 | 题库浏览 | 验证题库列表加载 | 打开题库页面 | 显示题号、标题、难度和标签 | 与期望一致 | 通过 |
| TC-FUNC-004 | 题库浏览 | 验证组合筛选 | 输入关键词并选择难度、标签和状态 | 列表按条件刷新 | 与期望一致 | 通过 |
| TC-FUNC-005 | 在线做题 | 验证题目详情加载 | 点击题目进入做题页 | 显示题面、限制、样例和代码编辑区 | 与期望一致 | 通过 |
| TC-FUNC-006 | 在线做题 | 验证代码草稿保存 | 编辑代码并保存草稿 | 再次进入页面可恢复草稿 | 与期望一致 | 通过 |
| TC-FUNC-007 | 代码运行 | 验证样例运行 | 使用正确代码点击运行 | 返回运行通过结果 | 与期望一致 | 通过 |
| TC-FUNC-008 | 提交判题 | 验证正式提交 | 使用正确代码点击提交 | 生成提交记录并返回AC结果 | 与期望一致 | 通过 |
| TC-FUNC-009 | 训练看板 | 验证训练数据展示 | 进入个人中心训练看板 | 显示题数、提交数、通过率和图表 | 与期望一致 | 通过 |
| TC-FUNC-010 | 后台管理 | 验证用户管理 | 管理员筛选和查看用户 | 用户列表和详情正常显示 | 与期望一致 | 通过 |
| TC-FUNC-011 | 后台管理 | 验证题目维护 | 新建或编辑题目 | 题目信息保存成功 | 与期望一致 | 通过 |
| TC-FUNC-012 | 后台管理 | 验证测试用例维护 | 新增、编辑或删除测试用例 | 测试用例数据保存成功 | 与期望一致 | 通过 |
| TC-FUNC-013 | 后台管理 | 验证标签管理 | 新增、编辑或删除标签 | 标签列表刷新正确 | 与期望一致 | 通过 |
| TC-FUNC-014 | 判题环境 | 验证环境检测 | 管理员打开判题环境检查 | 返回Docker和语言环境状态 | 与期望一致 | 通过 |

普通用户功能测试截图如图6-3至图6-7所示。登录弹窗能够正常提交账号信息，题库支持组合筛选，做题页能够展示题面和代码编辑区，训练看板能够显示用户训练统计。

![图6-3登录弹窗测试](../功能说明书/assets/screenshots/auth/auth-01-login-dialog.png)

图6-3登录弹窗测试

![图6-4题库组合筛选测试](../功能说明书/assets/screenshots/user/user-15-combined-filters.png)

图6-4题库组合筛选测试

![图6-5做题页代码编辑测试](../功能说明书/assets/screenshots/judge/judge-03-code-editor.png)

图6-5做题页代码编辑测试

![图6-6训练看板测试](../功能说明书/assets/screenshots/user/user-04-training-dashboard.png)

图6-6训练看板测试

![图6-7个人资料维护测试](../功能说明书/assets/screenshots/user/user-03-profile.png)

图6-7个人资料维护测试

管理员功能测试截图如图6-8至图6-12所示。管理员能够查看后台概览、维护用户、编辑题目、维护测试用例和管理标签。

![图6-8管理端概览测试](../功能说明书/assets/screenshots/admin/admin-01-overview.png)

图6-8管理端概览测试

![图6-9用户管理测试](../功能说明书/assets/screenshots/admin/admin-03-users-list.png)

图6-9用户管理测试

![图6-10题目列表管理测试](../功能说明书/assets/screenshots/admin/admin-06-problems-list.png)

图6-10题目列表管理测试

![图6-11题目编辑测试](../功能说明书/assets/screenshots/admin/admin-08-problem-edit-page.png)

图6-11题目编辑测试

![图6-12测试用例维护测试](../功能说明书/assets/screenshots/admin/admin-09-test-cases-edit.png)

图6-12测试用例维护测试

## 6.4接口测试

接口测试主要验证后端API的请求方法、路径、权限和返回结构是否符合设计。接口响应采用统一结构，包含业务码、消息、数据和时间戳等字段。核心接口测试结果如表6-3所示。

表6-3接口测试结果表

| 接口 | 方法 | 测试点 | 期望结果 | 结论 |
| --- | --- | --- | --- | --- |
| `/api/auth/login` | POST | 正确账号登录 | 返回用户信息和Token | 通过 |
| `/api/auth/register` | POST | 注册新用户 | 返回注册结果 | 通过 |
| `/api/auth/me` | GET | 当前用户信息 | 返回当前用户身份 | 通过 |
| `/api/problems` | GET | 题库分页查询 | 返回题目列表和分页字段 | 通过 |
| `/api/problems/no/{problemNo}` | GET | 题目详情 | 返回题面、限制和标签 | 通过 |
| `/api/problems/run` | POST | 运行代码 | 返回运行状态和输出 | 通过 |
| `/api/problems/no/{problemNo}/submissions` | POST | 正式提交 | 返回提交ID和状态 | 通过 |
| `/api/problems/submissions/{submissionId}` | GET | 提交详情 | 返回提交和用例结果 | 通过 |
| `/api/users/me/training-dashboard` | GET | 训练看板 | 返回训练统计数据 | 通过 |
| `/api/admin/users` | GET | 管理端用户列表 | 管理员可访问并返回分页数据 | 通过 |
| `/api/admin/problems` | GET | 管理端题目列表 | 管理员可访问并返回分页数据 | 通过 |
| `/api/admin/judge-environment/health` | GET | 判题环境检查 | 返回Docker检查结果 | 通过 |

登录接口、题库接口、运行接口和提交接口响应截图如图6-13至图6-16所示。

![图6-13登录接口响应测试](../功能说明书/assets/screenshots/api/api-01-login.png)

图6-13登录接口响应测试

![图6-14题库列表接口响应测试](../功能说明书/assets/screenshots/api/api-04-problem-list.png)

图6-14题库列表接口响应测试

![图6-15运行代码接口响应测试](../功能说明书/assets/screenshots/api/api-06-run-code.png)

图6-15运行代码接口响应测试

![图6-16提交代码接口响应测试](../功能说明书/assets/screenshots/api/api-07-submit-code.png)

图6-16提交代码接口响应测试

## 6.5判题流程测试

判题流程测试重点验证代码运行、正式提交、逐用例结果保存和用户进度更新。测试覆盖正确答案、答案错误、编译错误、运行错误和隐藏用例展示控制等情况。判题流程测试结果如表6-4所示。

表6-4判题流程测试表

| 用例编号 | 场景 | 输入 | 期望结果 | 实际结果 | 结论 |
| --- | --- | --- | --- | --- | --- |
| TC-JUDGE-001 | 样例运行通过 | 正确代码和样例输入 | 返回AC | 与期望一致 | 通过 |
| TC-JUDGE-002 | 样例运行答案错误 | 输出错误的代码 | 返回WA和实际输出 | 与期望一致 | 通过 |
| TC-JUDGE-003 | 编译错误 | 语法错误代码 | 返回CE和编译信息 | 与期望一致 | 通过 |
| TC-JUDGE-004 | 运行错误 | 运行时异常代码 | 返回RE和错误输出 | 与期望一致 | 通过 |
| TC-JUDGE-005 | 正式提交通过 | 正确代码 | 提交状态为AC，进度为SOLVED | 与期望一致 | 通过 |
| TC-JUDGE-006 | 正式提交失败 | 错误代码 | 提交状态为WA，进度为ATTEMPTED | 与期望一致 | 通过 |
| TC-JUDGE-007 | 隐藏用例展示 | 失败隐藏用例 | 隐藏输入和期望输出 | 与期望一致 | 通过 |

判题结果截图如图6-17至图6-20所示。系统能够展示通过、编译错误、运行错误和答案错误等状态，便于用户判断代码问题。

![图6-17提交AC详情测试](../功能说明书/assets/screenshots/judge/judge-39-submit-ac-detail.png)

图6-17提交AC详情测试

![图6-18编译错误结果测试](../功能说明书/assets/screenshots/judge/judge-36-run-compile-error.png)

图6-18编译错误结果测试

![图6-19运行错误结果测试](../功能说明书/assets/screenshots/judge/judge-37-run-runtime-error-stderr.png)

图6-19运行错误结果测试

![图6-20答案错误结果测试](../功能说明书/assets/screenshots/judge/judge-35-run-wrong-answer.png)

图6-20答案错误结果测试

## 6.6权限测试

权限测试用于验证匿名用户、普通用户和管理员在页面和接口访问上的边界。权限测试结果如表6-5所示。

表6-5权限测试表

| 用例编号 | 角色 | 操作 | 期望结果 | 实际结果 | 结论 |
| --- | --- | --- | --- | --- | --- |
| TC-AUTHZ-001 | 匿名用户 | 访问题库列表 | 允许访问 | 与期望一致 | 通过 |
| TC-AUTHZ-002 | 匿名用户 | 访问个人中心 | 跳转首页或要求登录 | 与期望一致 | 通过 |
| TC-AUTHZ-003 | 匿名用户 | 运行代码 | 拒绝访问 | 与期望一致 | 通过 |
| TC-AUTHZ-004 | 普通用户 | 访问管理员页面 | 拒绝访问 | 与期望一致 | 通过 |
| TC-AUTHZ-005 | 管理员 | 访问管理员页面 | 允许访问 | 与期望一致 | 通过 |
| TC-AUTHZ-006 | 管理员 | 发布题目 | 操作成功 | 与期望一致 | 通过 |

普通用户访问管理员页面被拒绝的截图如图6-21所示，管理员访问管理端成功的截图如图6-22所示。

![图6-21普通用户访问管理端被拒绝测试](../功能说明书/assets/screenshots/routes/routes-12-guard-user-admin-denied.png)

图6-21普通用户访问管理端被拒绝测试

![图6-22管理员访问管理端成功测试](../功能说明书/assets/screenshots/routes/routes-13-guard-admin-success.png)

图6-22管理员访问管理端成功测试

## 6.7判题环境检测测试

判题环境检测测试用于确认Docker服务、语言镜像和代码运行基础环境是否可用。管理员进入判题环境检查页面后，系统调用管理端健康检查接口，返回Docker状态、镜像状态和语言执行检查结果。当环境异常时，管理员可以根据检测结果判断是Docker未启动、镜像缺失还是语言运行命令异常。

判题环境检测截图如图6-23所示。

![图6-23判题环境健康检查测试](../功能说明书/assets/screenshots/admin/admin-02-judge-health.png)

图6-23判题环境健康检查测试

## 6.8自动化测试与测试结果分析

项目中已经配置后端JUnit测试、前端Vitest测试和Playwright端到端测试。后端测试覆盖认证、用户注册、密码重置、题库服务、提交服务、代码运行、判题环境和权限控制；前端单元测试覆盖接口封装、认证状态、路由守卫、题库筛选、做题页交互、个人中心和管理端组件；端到端测试覆盖登录、题号路由、题库分页、管理员题目流程和管理看板。

表6-6自动化测试覆盖表

| 测试类型 | 代表内容 | 覆盖目标 |
| --- | --- | --- |
| 后端单元/集成测试 | `SubmissionServiceImplTest`、`ProblemServiceImplTest`、`AuthPasswordResetControllerTest` | 业务服务、提交判题、密码重置、权限控制 |
| 前端单元测试 | `problemset-filters.spec.ts`、`problem-solve-actions.spec.ts`、`auth-store.spec.ts` | 页面交互、接口封装、状态管理 |
| 管理端前端测试 | `admin-user-management.spec.ts`、`admin-problem-management.spec.ts`、`TagManagement.test.ts` | 管理端列表、题目维护、标签管理 |
| 端到端测试 | `login.spec.ts`、`problem-pagination.spec.ts`、`admin-problem-flow.spec.ts` | 用户可见核心链路 |
| 构建检查 | `npm run build`、`mvn test` | 类型、构建和测试回归 |

从功能测试结果看，普通用户和管理员两条主线均能完成预期流程。普通用户可以完成登录、题库筛选、题目详情查看、代码编辑、草稿保存、代码运行、正式提交、提交记录查看和训练数据查看；管理员可以完成用户管理、题目维护、测试用例维护、标签管理、统计查看和判题环境检查。

从接口测试结果看，认证接口、题库接口、做题与判题接口、用户接口和管理员接口均能够返回结构化响应。分页接口能够返回题目列表和分页指标，提交接口能够返回提交状态和提交详情，管理端接口能够根据管理员权限正常访问。

从判题流程测试结果看，系统能够区分正确答案、答案错误、编译错误和运行错误，并能够保存逐用例结果。正式提交后，系统能够根据结果更新提交状态和用户题目进度。隐藏用例结果展示策略能够避免直接泄露隐藏输入和期望输出。

从权限测试结果看，系统能够区分匿名用户、普通用户和管理员。公开题库允许匿名访问，个人中心和判题操作要求登录，管理端页面和接口要求管理员角色，符合系统安全设计。综上，系统测试结果表明OJPT主要功能能够按照设计要求运行，满足毕业设计演示和论文验证需要。

<!-- pagebreak -->

# 7总结与展望

本文围绕OJPT在线编程训练平台完成了需求分析、系统设计、功能实现和系统测试。系统以普通用户在线训练和管理员后台维护为主线，实现了题库浏览、题目详情、代码编辑、代码草稿、样例运行、正式提交、提交结果、训练看板、个人资料维护、用户管理、题目管理、测试用例管理、标签管理、统计概览和判题环境健康检查等功能。通过这些功能，普通用户能够完成从题库筛选到在线做题、运行调试、正式提交和查看训练数据的完整训练流程，管理员能够完成题目、测试用例、标签、用户和判题环境的维护工作。

在技术实现方面，系统采用前后端分离架构。后端基于SpringBoot、Spring Security、MyBatis-Plus、MySQL、Redis、Flyway和Docker实现业务接口、权限控制、数据访问、数据库迁移和代码运行；前端基于Vue 3、TypeScript、Element Plus、Pinia和Axios实现单页应用、路由控制、状态维护和交互界面。系统通过JWT访问令牌和刷新令牌维护登录状态，通过USER和ADMIN角色区分普通用户和管理员权限，通过Docker隔离执行用户代码，使平台具备基本的在线判题能力和管理维护能力。

在软件工程建模方面，本文根据当前真实项目整理了系统功能模块图、系统用例图、顶层数据流图、零层数据流图、一层数据流图、系统部署结构图、实体属性图和数据库整体E-R图。同时，根据数据库迁移和后端接口整理了数据字典、数据库表结构、接口清单和测试用例表，使论文内容与系统实现保持一致。系统测试从功能测试、接口测试、判题流程测试、权限测试、判题环境检测和自动化测试等角度展开，测试结果表明系统主要功能能够按照设计要求运行。

虽然OJPT已经完成在线编程训练平台的核心闭环，但系统仍有进一步完善空间。当前判题调度能力主要面向本地演示环境，尚未形成面向大规模并发提交的分布式判题队列和弹性调度机制；竞赛、课程、作业和排行榜等组织能力尚未作为系统主线展开；题库内容管理仍可继续增加批量导入、题面版本记录、测试用例文件上传和题目审核流程；Docker判题环境已经提供基础隔离能力，但在网络隔离、文件系统限制、系统调用限制、资源配额和恶意代码防护方面仍可进一步强化；训练看板当前主要展示提交和题目进度，后续可以结合题目标签和提交历史提供更细粒度的能力分析。

后续优化可以从判题调度、安全沙盒、竞赛组织、题库管理和数据分析等方向展开。系统可以引入消息队列或任务调度组件，将提交创建、任务排队、代码执行和结果回写解耦，提高高并发提交场景下的吞吐能力和稳定性；可以在现有题库和提交功能基础上增加比赛、训练集、排行榜、限时提交和赛后统计，使系统适用于更多训练场景；可以完善题目批量导入、测试用例文件上传、题面版本记录和题目质量检查功能，提高管理员维护效率；也可以进一步限制容器权限、网络访问和文件系统访问，并记录判题日志、容器资源使用和异常原因，便于管理员定位问题。总体来看，OJPT已经达到毕业设计阶段对功能完整性、工程实现和测试验证的要求，后续仍可在平台化、规模化和智能化训练分析方面继续扩展。

<!-- pagebreak -->

<!-- pagebreak -->

# 参考文献

[1] 曾金,彭玲,毛志斌,张耀峰.一种分布式Online Judge系统设计与实现[J].软件导刊,2023,22(1):66-71.
[2] 李西明,梁志才,刘龙浩,祝胜林.基于微服务架构的在线评判系统设计与实现[J].软件导刊,2023,22(8):144-150.
[3] 陈义,唐郑熠,刘剑涛.面向应用型本科的在线编程训练系统的设计[J].电脑知识与技术,2024,20(19):57-59.
[4] 李菊,傅向华,马军超.基于代码编程规范的在线评测系统研究与实现[J].计算机时代,2023(1):62-65.
[5] 徐燕萍.基于OJ的程序设计类课程实验混合教学模式研究[J].软件导刊,2022,21(2):231-234.
[6] 金天成,窦亮,肖春芸,等.记忆与认知融合的个性化OJ习题推荐方法[J].计算机学报,2023,46(1):103-124.
[7] 严小雨,王明军,卓尧,等.基于开源技术的高校OJ系统部署运维与应用[J].现代信息科技,2024,8(3):89-93,98.
[8] 林世明,宋长军,李芳,等.赛教融合背景下基于OJ平台的高级程序设计课程教学改革:以昌吉学院信息工程学院为例[J].创新创业理论研究与实践,2024,7(10):42-46.
[9] 侯鹏哲.一种自动化测试系统的设计与实现[J].电脑编程技巧与维护,2024(7):16-18,39.
[10] 刘小玲,李慧云,殷珊珊,等.一种基于软件测试任务的信息化管理系统的原型设计[J].现代信息科技,2024,8(12):91-95.
[11] 赵超,刘洋洋.AI赋能的测试用例系统建设研究[J].现代信息科技,2025,9(23):121-125.
[12] 付春子,唐海涛,徐进.软件测试管理系统的研究与应用[J].现代计算机,2023,29(10):113-116.
[13] 郭瑞.基于Java试题资源库管理系统的设计与实现[J].电脑知识与技术,2025,21(13):45-47.
[14] 单树倩,任佳勋.基于SpringBoot和Vue框架的数据库原理网站设计与实现[J].电脑知识与技术,2021,17(30):40-41+50.
[15] 马雪山,张辉军,陈辉,等.前后端分离的Web平台技术研究与实现[J].电子技术与软件工程,2022(8):70-73.
[16] SHAO Jian-wei,LIANG Zhong-min,WANG Jun,et al.Design and Development of Medium and Long-term Hydrological Forecasting System Based on SpringBoot Framework[J].Water Resources and Power,2020,38(4):6-9+5.
[17] ZHANG Pu,TAO Li-na.A J2EE Web Application Framework Based on XML[J].Journal of Zhengzhou University(Natural Science Edition),2007(4):107-110.
[18] ZHANG Hong,WANG Hong.Workflow Management System Based on J2EE Technology[J].Journal of Zhengzhou University(Natural Science Edition),2007(3):93-96.
[19] LIU Zhen-yu,YANG Gen-xing,CAI Li-zhi.Software Test Case Generation with Adequacy Analysis on Scenario-Based Testing[J].Journal of Donghua University(English Edition),2011,28(2):139-144.
[20] HAN Ming,MIAO Chang-yun.Structured Query Language Injection Penetration Test Case Generation Based on Formal Description[J].Journal of Donghua University(English Edition),2015,32(3):446-452.

<!-- pagebreak -->

# 致谢

毕业论文能够顺利完成，离不开指导教师、学院老师、同学朋友以及家人的帮助与支持。首先，感谢指导教师王晨老师在论文选题、系统设计、实现推进和论文修改过程中给予的耐心指导。从开题阶段的研究方向梳理，到需求分析、系统架构、图表规范和论文表达的反复调整，老师都提出了具体而有针对性的意见，使我能够逐步把零散的开发工作整理为较为完整的毕业设计成果。

感谢计算机与自动化学院各位老师在本科阶段的教学与培养。专业课程学习为本系统的开发奠定了基础，特别是在Java Web开发、数据库设计、软件工程、前端开发、操作系统和测试相关课程中积累的知识，使我能够在OJPT在线编程训练平台的实现过程中完成前后端分离、权限控制、数据库建模、接口设计和判题流程测试等工作。

感谢同学和朋友在系统使用、界面体验和测试验证过程中提供的帮助。论文整理期间，他们对登录注册、题库浏览、在线做题、提交判题、后台管理等功能进行了试用，并提出了关于页面交互、异常提示和流程描述方面的建议，这些反馈帮助我发现并修正了部分不够清晰的实现细节，也使论文中的功能说明和测试记录更加贴近实际使用过程。

感谢家人在学习和毕业设计期间给予的理解、支持和鼓励。在论文写作和系统完善的过程中，家人的陪伴让我能够保持稳定的心态完成资料整理、功能调试和文档排版。毕业设计是本科阶段一次综合性的总结，也让我更加清楚地认识到软件开发需要持续学习、认真验证和耐心打磨。谨向所有给予我帮助的人表示诚挚的感谢。
