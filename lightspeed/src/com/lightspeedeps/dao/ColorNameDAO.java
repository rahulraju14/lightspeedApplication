//	File Name:	ColorNameDAO.java
package com.lightspeedeps.dao;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.model.ColorName;

/**
 * A data access object (DAO) providing persistence and search support for
 * ColorName entities. Transaction control of the save(), update() and delete()
 * operations can directly support Spring container-managed transactions or they
 * can be augmented to handle user-managed Spring transactions. Each of these
 * methods provides additional information for how to configure it for the
 * desired type of transaction control.
 *
 * @see com.lightspeedeps.model.ColorName
 */
public class ColorNameDAO extends BaseTypeDAO<ColorName> {
	private static final Log log = LogFactory.getLog(ColorNameDAO.class);
	// property constants
//	private static final String NAME = "name";
//	private static final String RGB_VALUE = "rgbValue";
	private static final String SCRIPT_REVISION = "scriptRevision";

	public static final ColorName WHITE = new ColorName("White", 0xffffff, 1);

	public static ColorNameDAO getInstance() {
		return (ColorNameDAO)getInstance("ColorNameDAO");
	}

	public List<ColorName> findByScriptRevision(Integer scriptRevision) {
		return findByProperty(SCRIPT_REVISION, scriptRevision);
	}

	/**
	 * @return A List of all the ColorNames which apply to script revisions.
	 * (The findAll method returns a list of all colors defined in the database; many of
	 * those may not be associated with script revision versions.)
	 */
	@SuppressWarnings("unchecked")
	public List<ColorName> findAllScriptColors() {
		log.debug("");
		String queryString = "from ColorName c where c.scriptRevision > 0";
		return find(queryString);
	}

	/**
	 * Find the color appropriate to the script revision specified, but always return
	 * a single ColorName (even a default).  Never returns null.
	 * @param revision - the script revision number
	 * @return A ColorName matching the revision number, or White if the database
	 * entry is not found for the requested revision.  If more than one entry is found
	 * (which should never be the case), the first one in the list is returned.
	 */
	public ColorName getColorByScriptRevision(int revision) {
		List<ColorName> list = findByScriptRevision(new Integer(revision));
		ColorName c = ColorNameDAO.WHITE;
		if (list != null && list.size() > 0) {
			c = list.get(0);
		}
		//log.debug(c);
		return c;
	}

//	public static ColorNameDAO getFromApplicationContext(ApplicationContext ctx) {
//		return (ColorNameDAO) ctx.getBean("ColorNameDAO");
//	}

}