SyntaxHighlighter.brushes.Custom = function()
{
    var keywords = 'INTEGER DOUBLE LONG BOOLEAN DATE DATETIME TEXT STRING ISTRING VARISTRING VARSTRING TIME RICHTEXT ' +
        'ABSTRACT ACTION ACTIVE ACTIVATE ADD ADDFORM ADDOBJ ADDSESSIONFORM AFTER ' +
        'AGGR AGGPROP AND APPLY AS ASON ASSIGN ASYNCUPDATE ATTACH '+
        'ATTR AUTO AUTOREFRESH AUTOSET BACKGROUND BCC BEFORE BOTTOM BREAK BY CANCEL ' +
        'CASE CC CENTER CHANGE CHANGECLASS CHANGED CHANGEWYS CHARSET CHECK ' +
        'CHECKED CLASS CLOSE COLOR COLUMNS COMPLEX CONCAT CONFIRM CONNECTION CONSTRAINT ' +
        'CONTAINERH CONTAINERV CONTEXTFILTER CSV CUSTOM CUSTOMFILE CYCLES DATA DBF DEFAULT DELAY DELETE ' +
        'DELETESESSION DESC DESIGN DIALOG DO DOC DOCKED DOCKEDMODAL DOCX DRAWROOT ' +
        'DROP DROPCHANGED DROPSET ECHO EDIT EDITABLE EDITFORM EDITKEY ' +
        'EDITSESSIONFORM ELSE EMAIL END EQUAL EVAL EVENTID EVENTS EXCELFILE ' +
        'EXCEPTLAST EXCLUSIVE EXEC EXPORT EXTEND FALSE FILE FILTER FILTERGROUP ' +
        'FILTERS FIRST FIXED FIXEDCHARWIDTH FOOTER FOR FORCE FOREGROUND ' +
        'FORM FORMS FORMULA FROM FULL FULLSCREEN GOAFTER GRID GROUP GROUPCHANGE HALIGN HEADER ' +
        'HIDE HIDESCROLLBARS HIDETITLE HINTNOUPDATE HINTTABLE HORIZONTAL ' +
        'HTML IF IMAGE IMAGEFILE IMPORT IMPOSSIBLE IN INCREMENT INDEX ' +
        'INDEXED INIT INITFILTER INLINE INPUT IS JDBC JOIN LAST LEADING LEFT LENGTH LIMIT ' +
        'LIST LOADFILE LOCAL LOGGABLE MANAGESESSION MAX MAXCHARWIDTH MDB ' +
        'MESSAGE META MIN MINCHARWIDTH MODAL MODULE MOVE MS MULTI NAGGR NAME NAMESPACE ' +
        'NAVIGATOR NEW NEWEXECUTOR NEWSESSION NEWSQL NEWTHREAD NO NOCANCEL NOHEADER NOHINT NOT NOWAIT NULL NUMERIC OBJECT ' +
        'OBJECTS OBJVALUE OK ON OPENFILE OPTIMISTICASYNC OR ORDER OVERRIDE PAGESIZE ' +
        'PANEL PARENT PARTITION PDF PDFFILE PERIOD PERSISTENT PG POSITION ' +
        'PREFCHARWIDTH PREV PRINT PRIORITY PROPERTIES PROPERTY ' +
        'PROPORTION QUERYOK QUERYCLOSE QUICKFILTER READ READONLY READONLYIF RECURSION REGEXP REMOVE ' +
        'REPORTFILES REQUEST REQUIRE RESOLVE RETURN RGB RIGHT ' +
        'ROUND RTF SAVEFILE SELECTOR SESSION SET SETCHANGED SCHEDULE SHORTCUT SHOW SHOWDROP ' +
        'SHOWIF SINGLE SHEET SPLITH SPLITV STEP STRETCH STRICT STRUCT SUBJECT ' +
        'SUM TAB TABBED TABLE TEXTHALIGN TEXTVALIGN THEN THREADS TIME TO TODRAW ' +
        'TOOLBAR TOP TRAILING TREE TRUE UNGROUP UPDATE VALIGN VALUE ' +
        'VERTICAL WHEN WHERE WHILE WINDOW WORDFILE WRITE XLS XLSX XML XOR YES';

    this.regexList = [
        { regex: SyntaxHighlighter.regexLib.singleLineCComments, css: 'color1' },
        { regex: /#{2,3}/gi, css: 'color2' },
        { regex: /@[a-zA-Z]\w*\b/gi, css: 'color2' },
        { regex: SyntaxHighlighter.regexLib.singleQuotedString, css: 'value' },
        { regex: /\b\d+l?\b/gi, css: 'value' },
        { regex: /\b\d+\.\d*d?\b/gi, css: 'value' },
        { regex: /\b\d{4}_\d\d_\d\d(_\d\d:\d\d)?\b/gi, css: 'value' },
        { regex: /\b\d\d:\d\d\b/gi, css: 'value' },
        { regex: /#[0-9a-fA-F]{6}/gi, css: 'value' },
        { regex: new RegExp(this.getKeywords(keywords), 'gm'), css: 'keyword' }
    ];
};

SyntaxHighlighter.brushes.Custom.prototype = new SyntaxHighlighter.Highlighter();
SyntaxHighlighter.brushes.Custom.aliases = ['custom', 'lsf', 'ls'];

