Stop='Stop'
 = New-Object -ComObject Word.Application
.Visible = False
.DisplayAlerts = 0
C:\Users\Administrator\Desktop\workspace\OJPT\.docs\论文\面向编程竞赛的SpringBoot在线算法题库系统的设计与实现论文——汪鸿韬.docx = .Documents.Open('C:\Users\Administrator\Desktop\workspace\OJPT\tmp\paper_review\paper_hidden.docx', False, True)
C:\Users\Administrator\Desktop\workspace\OJPT\.docs\论文\面向编程竞赛的SpringBoot在线算法题库系统的设计与实现论文——汪鸿韬.docx.SaveAs2('C:\Users\Administrator\Desktop\workspace\OJPT\tmp\paper_review\paper_hidden.pdf', 17)
C:\Users\Administrator\Desktop\workspace\OJPT\.docs\论文\面向编程竞赛的SpringBoot在线算法题库系统的设计与实现论文——汪鸿韬.docx.Close()
.Quit()
