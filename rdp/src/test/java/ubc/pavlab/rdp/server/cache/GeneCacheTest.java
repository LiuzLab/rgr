/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.cache;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.Taxon;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * Tests NcbiCacheImpl
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneCacheTest extends BaseSpringContextTest {

    @Autowired
    private GeneCache cache;

    private final static Taxon taxon = new Taxon( 9606L , "Homo sapiens", "human");
    private final static Taxon taxon2 = new Taxon( 562L , "Escherichia coli", "e. coli");

    /**
     * Initializes the cache and genes with sample data
     * 
     * @param cache
     */
    private static void initCache( GeneCache cache ) {
        Collection<Gene> genes = new HashSet<>();
        genes.add( new Gene( 1L, taxon, "aaa", "gene aa", "alias-a1,alias-a2" ) ); // match symbol exact first
        genes.add( new Gene( 2L, taxon, "aaaab", "gene ab", "alias-ab,alias-ab2" ) ); // match symbol partial
                                                                                        // second
        genes.add( new Gene( 3L, taxon, "dddd", "aaa gene dd", "alias-dd1,alias-dd2" ) ); // match name third
        genes.add( new Gene( 4L, taxon, "ccccc", "gene ccc", "alias-cc1,aaaalias-cc2" ) ); // match alias fourth
        genes.add( new Gene( 5L, taxon, "caaaa", "gene ca", "alias-ca1,alias-ca2" ) ); // not symbol suffix
        genes.add( new Gene( 6L, taxon, "bbb", "gene bbaaaa", "alias-b1" ) ); // not name suffix
        genes.add( new Gene( 7L, taxon2, "aaafish", "gene aa", "alias-a1,alias-a2" ) ); // not taxon

        cache.putAll( genes );
    }

    @Before
    public void setUp() {

        initCache( cache );
    }

    @After
    public void tearDown() {

        cache.clearAll();
    }

    /**
     * See Bug 4187
     */
    @Test
    public void testFetchByQuery() {
        List<Gene> results = (List) cache.fetchByQuery( "aaa", taxon.getId() );
        Long[] expectedNcbiGeneIds = new Long[] { 1L, 2L, 3L, 6L, 4L };
        assertEquals( expectedNcbiGeneIds.length, results.size() );
        assertEquals( 1L, results.get( 0 ).getId().longValue() );
        assertEquals( 2L, results.get( 1 ).getId().longValue() );

        // position 3 and 4 should be 3L and 6L in no particular order
        assertNotEquals( results.get( 2 ).getId(), results.get( 3 ).getId() );
        assertThat(results.get( 2 ).getId(), isOneOf(3L, 6L));
        assertThat(results.get( 3 ).getId(), isOneOf(3L, 6L));

        assertEquals( 4L, results.get( 4 ).getId().longValue() );

    }

    @Test
    public void testFetchBySymbolsAndTaxon() {
        Collection<String> symbols = new HashSet<>();
        symbols.add( "aaa" );
        symbols.add( "aaafish" );
        symbols.add( "aaaab" );
        Collection<Gene> results = cache.fetchBySymbolsAndTaxon( symbols, taxon.getId() );
        assertEquals( 2, results.size() );
    }

    @Test
    public void testFetchBySymbols() {
        Collection<String> symbols = new HashSet<>();
        symbols.add( "aaa" );
        symbols.add( "aaafish" );
        symbols.add( "aaaab" );
        Collection<Gene> results = cache.fetchBySymbols( symbols );
        assertEquals( 3, results.size() );
    }

    @Test
    public void testFetchByTaxons() {
        Collection<Long> taxons = new HashSet<>();
        taxons.add( taxon.getId() );
        Collection<Gene> results = cache.fetchByTaxons( taxons );
        assertEquals( 6, results.size() );

        taxons.add( taxon2.getId() );
        results = cache.fetchByTaxons( taxons );
        assertEquals( 7, results.size() );

        taxons.remove( taxon.getId() );
        results = cache.fetchByTaxons( taxons );
        assertEquals( 1, results.size() );
    }

    @Test
    public void testFetchById() {
        Collection<Long> ids = new HashSet<>();
        ids.add( 1L );
        ids.add( 3L );
        ids.add( 7L );
        ids.add( 8L );

        Collection<Gene> results = cache.fetchByIds( ids );
        assertEquals( 3, results.size() );
    }

    @Test
    public void testSize() {
        assertEquals( 7, cache.size() );
    }

    @Test
    public void testClearAll() {
        // This is dependent on testSize()
        assertEquals( 7, cache.size() );
        cache.clearAll();
        assertEquals( 0, cache.size() );

    }

}
