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
 * The Initial Developer of the Original Code is Ian Robertson.  Portions
 * created by Ian Robertson are Copyright (C) 2003 Ian Robertson.  All Rights
 * Reserved.
 *
 * Contributor(s):
 */

package org.jamon.integration;

import java.io.File;

import test.jamon.subdir.RelativePath;
import org.jamon.TemplateProcessor;
import org.jamon.JamonTemplateException;

public class RelativePathTest
    extends TestBase
{
    public void testRelativePath()
        throws Exception
    {
        new RelativePath(getTemplateManager()).render(getWriter());
        checkOutput("simple");
    }

    public void testToManyDotDots()
        throws Exception
    {
        expectTemplateException("TooManyDotDots",
                                "Cannot reference templates above the root",
                                1, 10);
    }
}
