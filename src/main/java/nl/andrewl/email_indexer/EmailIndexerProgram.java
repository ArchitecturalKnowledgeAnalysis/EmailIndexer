package nl.andrewl.email_indexer;

import nl.andrewl.email_indexer.data.EmailDataset;

import java.nio.file.Path;

public class EmailIndexerProgram {
	public static void main(String[] args) throws Exception {
//		new EmailDatasetGenerator().generate(
//				Path.of("A:\\Programming\\GitHub-andrewlalis\\ApacheEmailDownloader\\emails"),
//				Path.of("test-ds.zip")
//		);
		var ds = new EmailDataset(Path.of("test-ds.zip"));
		var stmt1 = ds.getConnection().prepareStatement("SELECT COUNT(MESSAGE_ID) FROM EMAIL");
		var rs = stmt1.executeQuery();
		rs.next();
		System.out.println(rs.getLong(1));
		stmt1.close();
		String q = "actor* availab* budget* business case* client* concern* conform* consisten* constraint* context* cost* coupl* customer* domain* driver* effort* enterprise* environment* experience* factor* force* function* goal* integrity interop* issue* latenc* maintain* manage* market* modifiab* objective* organization* performance* portab* problem* purpose* qualit* reliab* requirement* reus* safe* scal* scenario* secur* stakeholder* testab* throughput* usab* user* variability limit* time cohesion efficien* bandwidth speed* need* compat* complex* condition* criteria* resource* accura* complet* suitab* complianc* operabl* employabl* modular* analyz* readab* chang* encapsulat* transport* transfer* migrat* mova* replac* adapt* resilienc* irresponsib* stab* toleran* responsib* matur* accountab* vulnerab* trustworth* verif* protect* certificat* law* flexib* configur* convent* accessib* useful* learn* understand*";
//		for (var e : ds.search(q, 1, 1000).emails()) {
//			System.out.println(e.subject());
//		}
	}
}
