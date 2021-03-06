package ubc.pavlab.rdp.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ubc.pavlab.rdp.model.GeneInfo;
import ubc.pavlab.rdp.model.Taxon;

import java.util.Collection;

@Repository
public interface GeneInfoRepository extends JpaRepository<GeneInfo, Integer> {

    GeneInfo findByGeneId( Integer geneId );

    Collection<GeneInfo> findAllByIdIn( Collection<Integer> ids );

    GeneInfo findByGeneIdAndTaxon ( Integer geneId, Taxon taxon );

    GeneInfo findBySymbolAndTaxon( String symbol, Taxon taxon );

    Collection<GeneInfo> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon );

    Collection<GeneInfo> findAllBySymbol( String symbol );

    Collection<GeneInfo> findAllBySymbolStartingWithIgnoreCase( String symbolPrefix );

    Collection<GeneInfo> findAllByNameStartingWithIgnoreCase( String query );

    Collection<GeneInfo> findAllByAliasesContainingIgnoreCase( String query );
}
