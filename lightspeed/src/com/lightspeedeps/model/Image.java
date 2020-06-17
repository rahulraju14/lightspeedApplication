package com.lightspeedeps.model;

import java.util.Arrays;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.type.ImageType;

/**
 * Image entity, for storing pictures of other objects, such as
 * Contact`s, RealWorldElement`s, and ScriptElement`s.  Also used
 * to hold map images.
 */
@Entity
@Table(name = "image")
public class Image extends PersistentObject<Image> implements Cloneable, Comparable<Image> {
	private static final Log log = LogFactory.getLog(Image.class);

	private static final long serialVersionUID = 2499259869385589485L;

	// Fields

	/*
	 * An Image should normally have one (and only one) of the following
	 * "association" fields set, to indicate the object that it
	 * is a picture of or relates to.
	 */

	/** If non-null, this image is associated with the given RealWorldElement. */
	private RealWorldElement realWorldElement;

//	/** If non-null, this image is a map associated with the given RealWorldElement,
//	 * which should be of the type "LOCATION". */
//	private RealWorldElement locationElement;

	/** If non-null, this image is associated with the given ScriptElement. */
	private ScriptElement scriptElement;

	/** If non-null, this image is associated with the given PointOfInterest. */
	private PointOfInterest pointOfInterest;

	/** If non-null, this image is associated with the given Contact. That is,
	 * it is a picture of that person, maintained at the Production level. */
	private Contact contact;

	/** If non-null, this image is associated with the given User. That is,
	 * it is a picture of that person, and maintained at the "account" level
	 * and not the Production level. */
	private User user;

	/* (End of "association" fields.) */

	/** The title of the image (usually shorter than the Description);
	 * this usually contains the original filename of an uploaded image. */
	private String title;

	/** A description of the image -- this is not currently used. */
	private String description;

	/** The creator of the image.  This is currently set to the name of
	 * the User who uploads the image, so that it can be displayed in
	 * the timecard attachment description.  */
	private String artist;

	/** A date associated with the image.  This is currently set to
	 * the date when the image was first loaded into the application. */
	private Date date;

	/** The type of image. */
	private ImageType type;

	/** The image data that is a thumb-nail of the true image. */
	private byte[] thumbnail;

	/** The actual image data. */
	private byte[] content;

	// Constructors

	/** default constructor */
	public Image() {
		type = ImageType.OTHER;
	}

	/** full constructor */
/*	public Image(RealWorldElement realWorldElement,
			ScriptElement scriptElement, PointOfInterest pointOfInterest,
			String title, String description, String artist, Date date,
			String type, byte[] content) {
		this.realWorldElement = realWorldElement;
		this.scriptElement = scriptElement;
		this.pointOfInterest = pointOfInterest;
		this.title = title;
		this.description = description;
		this.artist = artist;
		this.date = date;
		this.type = type;
		this.content = content;
	}
*/
	// Property accessors

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Real_World_Element_Id")
	public RealWorldElement getRealWorldElement() {
		return realWorldElement;
	}
	public void setRealWorldElement(RealWorldElement realWorldElement) {
		this.realWorldElement = realWorldElement;
	}

//	/** See {@link #locationElement}. */
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "Location_Element_Id")
//	public RealWorldElement getLocationElement() {
//		return locationElement;
//	}
//	/** See {@link #locationElement}. */
//	public void setLocationElement(RealWorldElement locationElement) {
//		this.locationElement = locationElement;
//	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Script_Element_Id")
	public ScriptElement getScriptElement() {
		return scriptElement;
	}
	public void setScriptElement(ScriptElement scriptElement) {
		this.scriptElement = scriptElement;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Point_of_Interest_Id")
	public PointOfInterest getPointOfInterest() {
		return pointOfInterest;
	}
	public void setPointOfInterest(PointOfInterest pointOfInterest) {
		this.pointOfInterest = pointOfInterest;
	}

	/** See {@link #contact}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "Contact_Id")
	public Contact getContact() {
		return contact;
	}
	/** See {@link #contact}. */
	public void setContact(Contact contact) {
		this.contact = contact;
	}

	/** See {@link #user}. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "User_Id")
	public User getUser() {
		return user;
	}
	/** See {@link #user}. */
	public void setUser(User user) {
		this.user = user;
	}

	public static final int MAX_TITLE_LENGTH = 50;
	/** See {@link #title}. */
	@Column(name = "Title", length = 50)
	public String getTitle() {
		return title;
	}
	/** See {@link #title}. */
	public void setTitle(String title) {
		this.title = title;
	}

	/** Set the title field, after first shortening the provided
	 * string, if necessary, to the maximum allowed by the database.
	 * See {@link #title}. */
	public void setTruncatedTitle(String title) {
		if (title != null && title.length() > MAX_TITLE_LENGTH) {
			title = title.substring(0, MAX_TITLE_LENGTH);
		}
		this.title = title;
	}

	/** See {@link #description}. */
	@Column(name = "Description", length = 1000)
	public String getDescription() {
		return description;
	}
	/** See {@link #description}. */
	public void setDescription(String description) {
		this.description = description;
	}

	/** See {@link #artist}. */
	@Column(name = "Artist", length = 100)
	public String getArtist() {
		return artist;
	}
	/** See {@link #artist}. */
	public void setArtist(String artist) {
		this.artist = artist;
	}

	/** See {@link #date}. */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "Date", length = 0)
	public Date getDate() {
		return date;
	}
	/** See {@link #date}. */
	public void setDate(Date date) {
		this.date = date;
	}

	/** See {@link #type}. */
	@Enumerated(EnumType.STRING)
	@Column(name = "Type", nullable=false, length = 30)
	public ImageType getType() {
		return type;
	}
	/** See {@link #type}. */
	public void setType(ImageType type) {
		this.type = type;
	}

	/** See {@link #thumbnail}. */
	@Column(name = "Thumbnail")
	public byte[] getThumbnail() {
		return thumbnail;
		}
	/** See {@link #thumbnail}. */
	public void setThumbnail(byte[] content) {
		thumbnail = content;
	}

	@Transient
	public byte[] getThumbnailB() {
		if (thumbnail == null || thumbnail.length < 5) {
			return getContent();
		}
		return thumbnail;
	}

	/** See {@link #content}. */
	@Column(name = "Content")
	public byte[] getContent() {
		return content;
	}
	/** See {@link #content}. */
	public void setContent(byte[] content) {
		this.content = content;
	}

	@Override
	public int compareTo(Image arg) {
		int comp = getDate().compareTo(arg.getDate());
		return comp;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
		result = prime * result + ((getType() == null) ? 0 : getType().hashCode());
		result = prime * result + ((getDate() == null) ? 0 : getDate().hashCode());
		result = prime * result + ((getArtist() == null) ? 0 : getArtist().hashCode());
		result = prime * result + ((getDescription() == null) ? 0 : getDescription().hashCode());
		result = prime * result + ((getPointOfInterest() == null) ? 0 : getPointOfInterest().hashCode());
		result = prime * result + ((getRealWorldElement() == null) ? 0 : getRealWorldElement().hashCode());
		result = prime * result + ((getScriptElement() == null) ? 0 : getScriptElement().hashCode());
		result = prime * result + ((getContent() == null) ? 0 : Arrays.hashCode(getContent()));
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Image o;
		try {
			o = (Image)obj;
		}
		catch (Exception e) {
			return false;
		}
		if ( getId() != null && getId().equals(o.getId()) )
			return true;

		boolean b = false;
		b = (getType() == null && o.getType() == null) ||
			(getType() == o.getType());
		if (!b) return false;

		b = (getDate() == null && o.getDate() == null) ||
			(getDate() != null && getDate().equals(o.getDate()) );
		if (!b) return false;

		b = (getArtist() == null && o.getArtist() == null) ||
				(getArtist() != null && getArtist().equals(o.getArtist()) );
		if (!b) return false;

		b = (getDescription() == null && o.getDescription() == null) ||
				(getDescription() != null && getDescription().equals(o.getDescription()) );
		if (!b) return false;

		b = (getPointOfInterest() == null && o.getPointOfInterest() == null) ||
				(getPointOfInterest() != null && getPointOfInterest().equals(o.getPointOfInterest()));
		if (!b) return false;

		b = (getRealWorldElement() == null && o.getRealWorldElement() == null) ||
				(getRealWorldElement() != null && o.getRealWorldElement().equals(o.getRealWorldElement()));
		if (!b) return false;

		b = (getScriptElement() == null && o.getScriptElement() == null) ||
				(getScriptElement() != null && o.getScriptElement().equals(o.getScriptElement()));
		if (!b) return false;

		return Arrays.equals(getContent(), o.getContent());
	}

	@Override
	public Image clone() {
		Image image;
		try {
			image = (Image)super.clone();
			image.id = null;
		}
		catch (CloneNotSupportedException e) {
			log.error(e);
			return null;
		}
		return image;
	}

	@Override
	public String toString() {
		String s = super.toString();
		s += '[';
		s += "id=" + (getId()==null ? "null" : getId());
		if (getType() != null) {
			s += ", type=" + getType();
		}
		if (getDescription() != null) {
			s += ", desc=" + getDescription();
		}
		s += ']';
		return s;
	}

}