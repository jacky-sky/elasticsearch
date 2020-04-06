/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.analytics.ttest;

import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.search.DocValueFormat;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregatorFactories;
import org.elasticsearch.search.aggregations.AggregatorFactory;
import org.elasticsearch.search.aggregations.support.MultiValuesSourceAggregationBuilder;
import org.elasticsearch.search.aggregations.support.MultiValuesSourceAggregatorFactory;
import org.elasticsearch.search.aggregations.support.MultiValuesSourceFieldConfig;
import org.elasticsearch.search.aggregations.support.MultiValuesSourceParseHelper;
import org.elasticsearch.search.aggregations.support.ValueType;
import org.elasticsearch.search.aggregations.support.ValuesSource;
import org.elasticsearch.search.aggregations.support.ValuesSourceConfig;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class TTestAggregationBuilder extends MultiValuesSourceAggregationBuilder.LeafOnly<ValuesSource.Numeric, TTestAggregationBuilder> {
    public static final String NAME = "t_test";
    public static final ParseField A_FIELD = new ParseField("a");
    public static final ParseField B_FIELD = new ParseField("b");
    public static final ParseField TYPE_FIELD = new ParseField("type");
    public static final ParseField TAILS_FIELD = new ParseField("tails");

    public static final ObjectParser<TTestAggregationBuilder, String> PARSER =
        ObjectParser.fromBuilder(NAME, TTestAggregationBuilder::new);

    static {
        MultiValuesSourceParseHelper.declareCommon(PARSER, true, ValueType.NUMERIC);
        MultiValuesSourceParseHelper.declareField(A_FIELD.getPreferredName(), PARSER, true, false);
        MultiValuesSourceParseHelper.declareField(B_FIELD.getPreferredName(), PARSER, true, false);
        PARSER.declareString(TTestAggregationBuilder::testType, TYPE_FIELD);
        PARSER.declareInt(TTestAggregationBuilder::tails, TAILS_FIELD);

    }

    private TTestType testType = TTestType.HETEROSCEDASTIC;

    private int tails = 2;

    public TTestAggregationBuilder(String name) {
        super(name, ValueType.NUMERIC);
    }

    public TTestAggregationBuilder(TTestAggregationBuilder clone,
                                   AggregatorFactories.Builder factoriesBuilder,
                                   Map<String, Object> metadata) {
        super(clone, factoriesBuilder, metadata);
    }

    public TTestAggregationBuilder a(MultiValuesSourceFieldConfig valueConfig) {
        field(A_FIELD.getPreferredName(), Objects.requireNonNull(valueConfig, "Configuration for field [" + A_FIELD + "] cannot be null"));
        return this;
    }

    public TTestAggregationBuilder b(MultiValuesSourceFieldConfig weightConfig) {
        field(B_FIELD.getPreferredName(), Objects.requireNonNull(weightConfig, "Configuration for field [" + B_FIELD + "] cannot be null"));
        return this;
    }

    public TTestAggregationBuilder testType(String testType) {
        return testType(TTestType.resolve(Objects.requireNonNull(testType, "Test type cannot be null")));
    }

    public TTestAggregationBuilder testType(TTestType testType) {
        this.testType = Objects.requireNonNull(testType, "Test type cannot be null");
        return this;
    }

    public TTestAggregationBuilder tails(int tails) {
        if (tails < 1 || tails > 2) {
            throw new IllegalArgumentException(
                "[tails] must be 1 or 2. Found [" + tails + "] in [" + name + "]");
        }
        this.tails = tails;
        return this;
    }

    public TTestAggregationBuilder(StreamInput in) throws IOException {
        super(in, ValueType.NUMERIC);
        testType = in.readEnum(TTestType.class);
        tails = in.readVInt();
    }

    @Override
    protected AggregationBuilder shallowCopy(AggregatorFactories.Builder factoriesBuilder, Map<String, Object> metadata) {
        return new TTestAggregationBuilder(this, factoriesBuilder, metadata);
    }

    @Override
    public BucketCardinality bucketCardinality() {
        return BucketCardinality.NONE;
    }

    @Override
    protected void innerWriteTo(StreamOutput out) throws IOException {
        out.writeEnum(testType);
        out.writeVInt(tails);
    }


    @Override
    protected MultiValuesSourceAggregatorFactory<ValuesSource.Numeric> innerBuild(
        QueryShardContext queryShardContext,
        Map<String, ValuesSourceConfig<ValuesSource.Numeric>> configs,
        DocValueFormat format,
        AggregatorFactory parent,
        AggregatorFactories.Builder subFactoriesBuilder) throws IOException {
        return new TTestAggregatorFactory(name, configs, testType, tails, format, queryShardContext, parent, subFactoriesBuilder, metadata);
    }

    @Override
    public XContentBuilder doXContentBody(XContentBuilder builder, ToXContent.Params params) throws IOException {
        return builder;
    }

    @Override
    public String getType() {
        return NAME;
    }
}
