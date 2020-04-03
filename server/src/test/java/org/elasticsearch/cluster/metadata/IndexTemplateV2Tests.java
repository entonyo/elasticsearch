/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.cluster.metadata;

import org.elasticsearch.cluster.Diff;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.AbstractDiffableSerializationTestCase;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IndexTemplateV2Tests extends AbstractDiffableSerializationTestCase<IndexTemplateV2> {
    @Override
    protected IndexTemplateV2 makeTestChanges(IndexTemplateV2 testInstance) {
        try {
            return mutateInstance(testInstance);
        } catch (IOException e) {
            logger.error(e);
            fail("mutating should not throw an exception, but got: " + e);
            return null;
        }
    }

    @Override
    protected Writeable.Reader<Diff<IndexTemplateV2>> diffReader() {
        return IndexTemplateV2::readITV2DiffFrom;
    }

    @Override
    protected IndexTemplateV2 doParseInstance(XContentParser parser) throws IOException {
        return IndexTemplateV2.parse(parser);
    }

    @Override
    protected Writeable.Reader<IndexTemplateV2> instanceReader() {
        return IndexTemplateV2::new;
    }

    @Override
    protected IndexTemplateV2 createTestInstance() {
        return randomInstance();
    }

    public static IndexTemplateV2 randomInstance() {
        Settings settings = null;
        CompressedXContent mappings = null;
        Map<String, AliasMetadata> aliases = null;
        Template template = null;
        if (randomBoolean()) {
            if (randomBoolean()) {
                settings = randomSettings();
            }
            if (randomBoolean()) {
                mappings = randomMappings();
            }
            if (randomBoolean()) {
                aliases = randomAliases();
            }
            template = new Template(settings, mappings, aliases);
        }

        Map<String, Object> meta = null;
        if (randomBoolean()) {
            meta = randomMeta();
        }

        List<String> indexPatterns = randomList(1, 4, () -> randomAlphaOfLength(4));
        List<String> componentTemplates = randomList(0, 10, () -> randomAlphaOfLength(5));
        return new IndexTemplateV2(indexPatterns,
            template,
            componentTemplates,
            randomBoolean() ? null : randomNonNegativeLong(),
            randomBoolean() ? null : randomNonNegativeLong(),
            meta);
    }

    private static Map<String, AliasMetadata> randomAliases() {
        String aliasName = randomAlphaOfLength(5);
        AliasMetadata aliasMeta = AliasMetadata.builder(aliasName)
            .filter(Collections.singletonMap(randomAlphaOfLength(2), randomAlphaOfLength(2)))
            .routing(randomBoolean() ? null : randomAlphaOfLength(3))
            .isHidden(randomBoolean() ? null : randomBoolean())
            .writeIndex(randomBoolean() ? null : randomBoolean())
            .build();
        return Collections.singletonMap(aliasName, aliasMeta);
    }

    private static CompressedXContent randomMappings() {
        try {
            return new CompressedXContent("{\"" + randomAlphaOfLength(3) + "\":\"" + randomAlphaOfLength(7) + "\"}");
        } catch (IOException e) {
            fail("got an IO exception creating fake mappings: " + e);
            return null;
        }
    }

    private static Settings randomSettings() {
        return Settings.builder()
            .put(randomAlphaOfLength(4), randomAlphaOfLength(10))
            .build();
    }

    private static Map<String, Object> randomMeta() {
        if (randomBoolean()) {
            return Collections.singletonMap(randomAlphaOfLength(4), randomAlphaOfLength(4));
        } else {
            return Collections.singletonMap(randomAlphaOfLength(5),
                Collections.singletonMap(randomAlphaOfLength(4), randomAlphaOfLength(4)));
        }
    }

    @Override
    protected IndexTemplateV2 mutateInstance(IndexTemplateV2 orig) throws IOException {
        return mutateTemplate(orig);
    }

    public static IndexTemplateV2 mutateTemplate(IndexTemplateV2 orig) {
        switch (randomIntBetween(0, 5)) {
            case 0:
                List<String> newIndexPatterns = randomValueOtherThan(orig.indexPatterns(),
                    () -> randomList(1, 4, () -> randomAlphaOfLength(4)));
                return new IndexTemplateV2(newIndexPatterns, orig.template(), orig.composedOf(),
                    orig.priority(), orig.version(), orig.metadata());
            case 1:
                return new IndexTemplateV2(orig.indexPatterns(),
                    randomValueOtherThan(orig.template(), () -> new Template(randomSettings(), randomMappings(), randomAliases())),
                    orig.composedOf(),
                    orig.priority(),
                    orig.version(),
                    orig.metadata());
            case 2:
                List<String> newComposedOf = randomValueOtherThan(orig.composedOf(),
                    () -> randomList(0, 10, () -> randomAlphaOfLength(5)));
                return new IndexTemplateV2(orig.indexPatterns(),
                    orig.template(),
                    newComposedOf,
                    orig.priority(),
                    orig.version(),
                    orig.metadata());
            case 3:
                return new IndexTemplateV2(orig.indexPatterns(),
                    orig.template(),
                    orig.composedOf(),
                    randomValueOtherThan(orig.priority(), ESTestCase::randomNonNegativeLong),
                    orig.version(),
                    orig.metadata());
            case 4:
                return new IndexTemplateV2(orig.indexPatterns(),
                    orig.template(),
                    orig.composedOf(),
                    orig.priority(),
                    randomValueOtherThan(orig.version(), ESTestCase::randomNonNegativeLong),
                    orig.metadata());
            case 5:
                return new IndexTemplateV2(orig.indexPatterns(),
                    orig.template(),
                    orig.composedOf(),
                    orig.priority(),
                    orig.version(),
                    randomValueOtherThan(orig.metadata(), IndexTemplateV2Tests::randomMeta));
            default:
                throw new IllegalStateException("illegal randomization branch");
        }
    }
}
