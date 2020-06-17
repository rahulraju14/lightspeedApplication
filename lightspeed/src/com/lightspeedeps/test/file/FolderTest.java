package com.lightspeedeps.test.file;

import java.io.File;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lightspeedeps.dao.DocumentChainDAO;
import com.lightspeedeps.dao.DocumentDAO;
import com.lightspeedeps.dao.FolderDAO;
import com.lightspeedeps.dao.PacketDAO;
import com.lightspeedeps.dao.ProductionDAO;
import com.lightspeedeps.dao.UserDAO;
import com.lightspeedeps.model.Document;
import com.lightspeedeps.model.DocumentChain;
import com.lightspeedeps.model.Folder;
import com.lightspeedeps.model.Packet;
import com.lightspeedeps.model.Production;
import com.lightspeedeps.model.User;
import com.lightspeedeps.test.SpringMultiTestCase;
import com.lightspeedeps.type.MimeType;
import com.lightspeedeps.util.file.FileRepositoryUtils;
import com.lightspeedeps.web.util.ApplicationScopeBean;

public class FolderTest extends SpringMultiTestCase {

	private static Log log = LogFactory.getLog(FolderTest.class);

	String newFolderName = "$NewTestFolder";
	String renameTestFolder ="$RenameTestFolder";
	String newPacket = "$NewPacket";
	String renamePacket = "$RenamePacket";

	private final static String PRODUCTION = "MyProduction";

	private final static String PATH =
//			"/home/ist-126/lightspeed/LightSpeedLocal/lightspeed29/junit/data/";
//			".\\data\\"; // use this for JUnit batch testing - relative to {project}/junit/
			"./junit/data/"; // use this for Eclipse JUnit testing - relative to {project}

	private final static String FILENAME = "test_script";
	private final static String FILETYPE = ".pdf";

	ProductionDAO productionDAO;
	FolderDAO folderDAO;
	Production prod;
	Folder onboardFolder;
	PacketDAO packetDAO;

	public void init() {
		try {
			productionDAO = ProductionDAO.getInstance();
			folderDAO = FolderDAO.getInstance();
			prod = productionDAO.findOneByProperty(ProductionDAO.TITLE, PRODUCTION);
			onboardFolder = FileRepositoryUtils.findFolder("Onboarding", prod.getRootFolder());
			packetDAO = PacketDAO.getInstance();
		}
		catch (Exception e) {
			log.error("init failed");
		}
	}

	public void testFolderCreation () {
		init();

		Integer parentId = onboardFolder.getId();
		folderDAO.createFolder(newFolderName, parentId, false);
		Folder createdFfolder = folderDAO.findOneByProperty("name", newFolderName);

		assertNotNull(createdFfolder);
		assertEquals("compare name of the folder", newFolderName, newFolderName);
	}

	public void testFolderRename () {
		init();

		Folder createdFolder = folderDAO.findOneByProperty("name", newFolderName);

		folderDAO.rename(createdFolder, renameTestFolder);
		Folder renamedFfolder = folderDAO.findOneByProperty("name", renameTestFolder);

		assertNotNull(renamedFfolder);
		assertEquals("compare the name of the folder", renameTestFolder, renamedFfolder.getName());
	}

	public void testFolderMove () {
		init();

		Folder createdFolder = folderDAO.findOneByProperty("name", renameTestFolder);
		folderDAO.move(createdFolder, onboardFolder);
		Folder movedFolder = folderDAO.findFolderByParentIdAndName(renameTestFolder, onboardFolder.getId());

		assertNotNull(movedFolder);
		assertEquals("compare folder's parent id", createdFolder.getFolder().getId(), movedFolder.getFolder().getId());
	}

	public void testClearFolder () {
		init();

		Folder folder = folderDAO.findOneByProperty("name", renameTestFolder);
		if (folder != null) {
			folderDAO.remove(folder);
		}
	}

	public void testDocumentUpload () {
		init();

		DocumentDAO documentDAO = DocumentDAO.getInstance();
		DocumentChainDAO documentChainDAO = DocumentChainDAO.getInstance();

		User user = UserDAO.getInstance().findById(1);
		File file = new File(PATH + FILENAME + FILETYPE);
		Integer documentId = FileRepositoryUtils.storeFile(onboardFolder, FILENAME, user, file,
				MimeType.PDF, "pdf", new Date(), false);

		Document document = documentDAO.findOneByProperty("name", FILENAME);
		assertNotNull(document);
		assertEquals(FILENAME,document.getName());
		documentDAO.deleteDocument(documentId);

		Integer docChainId = documentChainDAO.saveDocumentChain(onboardFolder, FILENAME, "test", user, MimeType.PDF, "NONE", new Date());
		DocumentChain docChain = documentChainDAO.findById(docChainId);
		assertNotNull(docChain);
		assertEquals(FILENAME,docChain.getName());
		documentChainDAO.deleteDocumentChain(docChain, onboardFolder);

		if (ApplicationScopeBean.getInstance().getJunit()) {
			log.debug("JUnit test running");
		}
	}

	/**
	 * This should be the last test method in the class, so that the JUnit test
	 * structure will run it after all the other tests. It calls the superclass
	 * method to clean up (release) the Spring/Hibernate environment.
	 *
	 * @throws Exception
	 */
	public void testSpringTearDown() throws Exception {
		springTearDown();
	}

	public void testCreatePacket() {
		init();

		Integer packetId = packetDAO.create(newPacket, null);
		Packet packet = packetDAO.findById(packetId);

		assertNotNull(packet);
		assertEquals("compare the newly created packets name", newPacket, packet.getName());
	}

	public void testRenamePacket() {
		init();

		Packet packet = packetDAO.findOneByProperty("name", newPacket);
		packet.setName(renamePacket);
		packetDAO.attachDirty(packet);
		packet = packetDAO.findById(packet.getId());

		assertNotNull(packet);
		assertEquals("compare the renamed packet name", renamePacket, packet.getName());
	}

	public void testDeletionPacket() {
		init();

		Packet packet = packetDAO.findOneByProperty("name", renamePacket);
		packetDAO.delete(packet);
		packet = packetDAO.findOneByProperty("name", renamePacket);

		assertNull("check to see the packet is deleted or not", packet);
	}
}
