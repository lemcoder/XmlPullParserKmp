package io.github.lemcoder.exceptions

import io.github.lemcoder.XmlPullParser

/**
 * This exception is thrown to signal XML Pull Parser related faults.
 *
 * @author [Aleksander Slominski](http://www.extreme.indiana.edu/~aslom/)
 */
class XmlPullParserException : Exception {
    protected var detail: Throwable? = null

    // public void setDetail(Throwable cause) { this.detail = cause; }
    var lineNumber: Int = -1
        protected set

    var columnNumber: Int = -1
        protected set

    /*
     * public XmlPullParserException() { }
     */
    constructor(s: String?) : super(s)

    /*
     * public XmlPullParserException(String s, Throwable throwable) { super(s); this.detail = throwable; } public
     * XmlPullParserException(String s, int row, int column) { super(s); this.row = row; this.column = column; }
     */
    constructor(msg: String?, parser: XmlPullParser?, chain: Throwable?) : super(
        ((if (msg == null) "" else "$msg ")
                + (if (parser == null) "" else "(position:" + parser.positionDescription + ") ")
                + (if (chain == null) "" else "caused by: $chain")),
        chain
    ) {
        if (parser != null) {
            this.lineNumber = parser.lineNumber
            this.columnNumber = parser.columnNumber
        }
        this.detail = chain
    }
}