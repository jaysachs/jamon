/*
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Jamon code, released February, 2003.
 *
 * The Initial Developer of the Original Code is Jay Sachs.  Portions
 * created by Jay Sachs are Copyright (C) 2003 Jay Sachs.  All Rights
 * Reserved.
 *
 * Contributor(s): Ian Robertson
 */

package org.jamon.codegen;

import java.io.File;
import java.io.IOException;

import org.jamon.JamonTemplateException;
import org.jamon.parser.ParserException;
import org.jamon.lexer.LexerException;
import org.jamon.node.Token;

public class JamonParseException
    extends JamonTemplateException
{
    private static class Details
    {
        Details(int p_line, int p_column, String p_desc)
        {
            line = p_line;
            column = p_column;
            description = p_desc;
        }
        final int line;
        final int column;
        final String description;
    }

    private JamonParseException(String p_fileName, Details p_details)
    {
        super(p_details.description,
              p_fileName,
              p_details.line,
              p_details.column);
    }

    public JamonParseException(String p_fileName,
                               EncodingReader.Exception p_exception)
    {
        this(p_fileName,
             new Details(1, p_exception.getPos(), p_exception.getMessage()));
    }

    public JamonParseException(String p_fileName, LexerException p_exception)
    {
        this(p_fileName, parseLexerException(p_exception));
    }

    public JamonParseException(String p_fileName, ParserException p_exception)
    {
        this(p_fileName, parseParserException(p_exception));
    }

    private static Details parseLexerException(LexerException p_exception)
    {
        String message = p_exception.getMessage();
        int i = message.indexOf(',');
        int j = message.indexOf(']');
        return new Details(Integer.parseInt(message.substring(1, i)),
                           Integer.parseInt(message.substring(i+1,j)),
                           message.substring(j+2));
    }

    private static Details parseParserException(ParserException p_exception)
    {
        Token token = p_exception.getToken();
        int i = p_exception.getMessage().lastIndexOf(']');
        return new Details(token.getLine(),
                           token.getPos(),
                           p_exception.getMessage().substring(i+1));
    }

    public String getStandardMessage()
    {
        return getFileName() + ":" + getLine() + ":" + getColumn() + ":"
            + getMessage();
    }
}


/*
  "[" + (start_line + 1) + "," + (start_pos + 1) + "]" +
  " Unknown token: " + text);
*/
