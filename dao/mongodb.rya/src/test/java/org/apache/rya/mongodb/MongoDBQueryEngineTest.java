package org.apache.rya.mongodb;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.rya.api.RdfCloudTripleStoreConfiguration;
import org.apache.rya.api.RdfCloudTripleStoreUtils;
import org.apache.rya.api.domain.RyaStatement;
import org.apache.rya.api.domain.RyaStatement.RyaStatementBuilder;
import org.apache.rya.api.domain.RyaURI;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.impl.MapBindingSet;

import com.google.common.collect.Lists;

import info.aduna.iteration.CloseableIteration;

public class MongoDBQueryEngineTest extends MongoRyaTestBase {

    // private dao;
    // private configuration;

    private MongoDBQueryEngine engine;
    private MongoDBRdfConfiguration configuration;

    @Before
    public void setUp() throws Exception {
        // Set up Mongo/Rya
        final Configuration conf = new Configuration();
        conf.set(MongoDBRdfConfiguration.MONGO_DB_NAME, getDbName());
        conf.set(MongoDBRdfConfiguration.MONGO_COLLECTION_PREFIX, "rya_");
        conf.set(RdfCloudTripleStoreConfiguration.CONF_TBL_PREFIX, "rya_");
        configuration = new MongoDBRdfConfiguration(conf);
        final int port = mongoClient.getServerAddressList().get(0).getPort();
        configuration.set(MongoDBRdfConfiguration.MONGO_INSTANCE_PORT, Integer.toString(port));

        engine = new MongoDBQueryEngine(configuration, mongoClient);

        // Add Data
        final MongoDBRyaDAO dao = new MongoDBRyaDAO(configuration, mongoClient);
        dao.add(getStatement("u:a", "u:tt", "u:b"));
        dao.add(getStatement("u:a", "u:tt", "u:c"));
    }

    private RyaStatement getStatement(final String s, final String p, final String o) {
        final RyaStatementBuilder builder = new RyaStatementBuilder();
        if (s != null) {
            builder.setSubject(new RyaURI(s));
        }
        if (p != null) {
            builder.setPredicate(new RyaURI(p));
        }
        if (o != null) {
            builder.setObject(new RyaURI(o));
        }
        return builder.build();
    }

    public int size(final CloseableIteration<?, ?> iter) throws Exception {
        int i = 0;
        while (iter.hasNext()) {
            i++;
            iter.next();
        }
        return i;
    }

    @Test
    public void statementQuery() throws Exception {
        final RyaStatement s = getStatement("u:a", null, null);
        Assert.assertEquals(2, size(engine.query(s, configuration)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void bindingSetsQuery() throws Exception {
        final RyaStatement s = getStatement("u:a", null, null);

        final MapBindingSet bs1 = new MapBindingSet();
        bs1.addBinding("foo", new URIImpl("u:x"));

        final Map.Entry<RyaStatement, BindingSet> e1 = new RdfCloudTripleStoreUtils.CustomEntry<RyaStatement, BindingSet>(s, bs1);
        final Collection<Entry<RyaStatement, BindingSet>> stmts1 = Lists.newArrayList(e1);
        Assert.assertEquals(2, size(engine.queryWithBindingSet(stmts1, configuration)));


        final MapBindingSet bs2 = new MapBindingSet();
        bs2.addBinding("foo", new URIImpl("u:y"));

        final Map.Entry<RyaStatement, BindingSet> e2 = new RdfCloudTripleStoreUtils.CustomEntry<RyaStatement, BindingSet>(s, bs2);

        final Collection<Entry<RyaStatement, BindingSet>> stmts2 = Lists.newArrayList(e1, e2);
        Assert.assertEquals(4, size(engine.queryWithBindingSet(stmts2, configuration)));
}
}
