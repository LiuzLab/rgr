INSERT IGNORE INTO role VALUES (1,'ROLE_ADMIN');
INSERT IGNORE INTO role VALUES (2,'ROLE_USER');

## Default admin password is: defaultadminpwd26
INSERT IGNORE INTO user (user_id, email, enabled, password, privacy_level, description, last_name, name, shared, hide_genelist)
VALUES(3, CONCAT(RAND(),"@msl.ubc.ca_notreal"), 0, MD5(RAND()), 0, "Remote admin profile", "", "", false, false);

INSERT IGNORE INTO user_role (user_id,role_id) VALUES (3, 1);
INSERT IGNORE INTO user_role (user_id,role_id) VALUES (3, 2);

INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (9606, "human", "Homo sapiens", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Homo_sapiens.gene_info.gz", true, 1);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (10090, "mouse", "Mus musculus", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Mus_musculus.gene_info.gz", false, 2);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (10116, "rat", "Rattus norvegicus", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/Rattus_norvegicus.gene_info.gz", false, 3);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (559292, "yeast", "Saccharomyces cerevisiae S288c", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Fungi/Saccharomyces_cerevisiae.gene_info.gz", false, 7);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (7955, "zebrafish", "Danio rerio", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Non-mammalian_vertebrates/Danio_rerio.gene_info.gz", false, 4);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (7227, "fruit fly", "Drosophila melanogaster", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Drosophila_melanogaster.gene_info.gz", false, 5);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (6239, "roundworm", "Caenorhabditis elegans", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Invertebrates/Caenorhabditis_elegans.gene_info.gz", false, 6);
INSERT IGNORE INTO taxon (taxon_id, common_name, scientific_name, gene_url, active, ordering) VALUES (511145, "e. coli", "Escherichia coli", "ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Archaea_Bacteria/Escherichia_coli_str._K-12_substr._MG1655.gene_info.gz", false, 8);
