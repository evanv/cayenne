package org.objectstyle.cayenne;
/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */

import junit.framework.Assert;

import org.apache.oro.text.perl.Perl5Util;

public class TranslationTestCase {
	public static final Perl5Util regexUtil = new Perl5Util();

	public static final String ALIAS_TOKEN = "<ta.>";
	// private static final Pattern aliasPattern = Pattern.compile("\\b\\w+\\.");
	// private static final Pattern aliasStripPattern = Pattern.compile("<ta\\.>");

	protected Object tstObject;
	protected String sqlExp;
	protected String sqlExpNoAlias;
	protected String rootEntity;

	public TranslationTestCase(
		String rootEntity,
		Object tstObject,
		String sqlExp) {
		this.tstObject = tstObject;
		this.sqlExp = sqlExp;
		this.rootEntity = rootEntity;

		sqlExpNoAlias = trim("<ta\\.>", sqlExp);
	}

	protected String trim(String pattern, String str) {
		return trim(pattern, str, "");
	}

	protected String trim(String pattern, String str, String subst) {
		return (regexUtil.match("/" + pattern + "/", sqlExp))
			? regexUtil.substitute("s/" + pattern + "<ta\\.>/" + subst + "/", sqlExp)
			: str;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(this.getClass().getName()).append(tstObject);
		return buf.toString();
	}

	public void assertTranslatedWell(String translated, boolean usedAliases) {
		if (sqlExp == null) {
			Assert.assertNull(translated);
			return;
		}

		Assert.assertNotNull(translated);

		if (usedAliases) {
			// replace column aliases with dummy string 
			String aliasSubstituted =
				trim("\\b\\w+\\.", translated, ALIAS_TOKEN);
			Assert.assertEquals(sqlExp, aliasSubstituted);
		} else {
			// strip column aliases
			String aliasSubstituted = trim("\\b\\w+\\.", translated);
			Assert.assertEquals(sqlExpNoAlias, aliasSubstituted);
		}
	}

	public String getRootEntity() {
		return rootEntity;
	}

	public String getSqlExp() {
		return sqlExp;
	}
}
