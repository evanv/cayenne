/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.project.validator;

import java.util.Iterator;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DerivedDbEntity;
import org.objectstyle.cayenne.project.ProjectPath;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DbEntityValidator extends TreeNodeValidator {

	/**
	 * Constructor for DbEntityValidator.
	 */
	public DbEntityValidator() {
		super();
	}

	public void validateObject(ProjectPath path, Validator validator) {
		DbEntity ent = (DbEntity) path.getObject();
		validateName(ent, path, validator);
		validateAttributes(ent, path, validator);
		validatePK(ent, path, validator);

		if ((ent instanceof DerivedDbEntity)
			&& ((DerivedDbEntity) ent).getParentEntity() == null) {
			validator.registerError(
				"No parent selected for derived entity \""
					+ ent.getName()
					+ "\".",
				path);
		}
	}

	/**
	 * Validates the presence of the primary key. A warning is given only if the parent
	 * map also conatins an ObjEntity mapped to this entity, since unmapped primary key
	 * is ok if working with data rows.
	 */
	protected void validatePK(
		DbEntity ent,
		ProjectPath path,
		Validator validator) {
		if (ent.getAttributes().size() > 0
			&& ent.getPrimaryKey().size() == 0) {
			DataMap map = ent.getDataMap();
			if (map != null && map.getMappedEntities(ent).size() > 0) {
				// there is an objentity, so complain about no pk
				validator.registerWarning(
					"DbEntity \""
						+ ent.getName()
						+ "\" has no primary key attributes defined.",
					path);
			}
		}
	}

	/**
	 * Tables must have columns.
	 */
	protected void validateAttributes(
		DbEntity ent,
		ProjectPath path,
		Validator validator) {
		if (ent.getAttributes().size() == 0) {
			// complain about missing attributes
			validator.registerWarning(
				"DbEntity \"" + ent.getName() + "\" has no attributes defined.",
				path);
		}
	}

	protected void validateName(
		DbEntity ent,
		ProjectPath path,
		Validator validator) {
		String name = ent.getName();

		// Must have name
		if (Util.isEmptyString(name)) {
			validator.registerError("Unnamed DbEntity.", path);
			return;
		}

		DataMap map = (DataMap) path.getObjectParent();
		if (map == null) {
			return;
		}

		// check for duplicate names in the parent context
		Iterator it = map.getDbEntities().iterator();
		while (it.hasNext()) {
			DbEntity otherEnt = (DbEntity) it.next();
			if (otherEnt == ent) {
				continue;
			}

			if (name.equals(otherEnt.getName())) {
				validator.registerError(
					"Duplicate DbEntity name: " + name + ".",
					path);
				break;
			}
		}
	}
}
