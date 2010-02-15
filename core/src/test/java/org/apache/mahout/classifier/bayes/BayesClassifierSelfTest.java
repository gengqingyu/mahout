/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.classifier.bayes;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.mahout.classifier.ClassifierData;
import org.apache.mahout.classifier.ClassifierResult;
import org.apache.mahout.classifier.ResultAnalyzer;
import org.apache.mahout.classifier.bayes.algorithm.BayesAlgorithm;
import org.apache.mahout.classifier.bayes.algorithm.CBayesAlgorithm;
import org.apache.mahout.classifier.bayes.common.BayesParameters;
import org.apache.mahout.classifier.bayes.datastore.InMemoryBayesDatastore;
import org.apache.mahout.classifier.bayes.exceptions.InvalidDatastoreException;
import org.apache.mahout.classifier.bayes.interfaces.Algorithm;
import org.apache.mahout.classifier.bayes.interfaces.Datastore;
import org.apache.mahout.classifier.bayes.mapreduce.bayes.BayesClassifierDriver;
import org.apache.mahout.classifier.bayes.model.ClassifierContext;
import org.apache.mahout.common.MahoutTestCase;
import org.apache.mahout.common.nlp.NGrams;

public class BayesClassifierSelfTest extends MahoutTestCase {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ClassifierData.writeDataToFile("testdata/bayesinput", ClassifierData.DATA);
  }
  
  public void testSelfTestBayes() throws InvalidDatastoreException, IOException {
    BayesParameters params = new BayesParameters(1);
    params.set("alpha_i", "1.0");
    params.set("dataSource", "hdfs");
    TrainClassifier.trainNaiveBayes("testdata/bayesinput", "testdata/bayesmodel", params);
    
    params.set("verbose", "true");
    params.set("basePath", "testdata/bayesmodel");
    params.set("classifierType", "bayes");
    params.set("dataSource", "hdfs");
    params.set("defaultCat", "unknown");
    params.set("encoding", "UTF-8");
    params.set("alpha_i", "1.0");
    
    Algorithm algorithm = new BayesAlgorithm();
    Datastore datastore = new InMemoryBayesDatastore(params);
    ClassifierContext classifier = new ClassifierContext(algorithm, datastore);
    classifier.initialize();
    ResultAnalyzer resultAnalyzer = new ResultAnalyzer(classifier.getLabels(), params.get("defaultCat"));
    
    for (String[] entry : ClassifierData.DATA) {
      List<String> document = new NGrams(entry[1], Integer.parseInt(params.get("gramSize")))
          .generateNGramsWithoutLabel();
      assertEquals(3, classifier.classifyDocument(document.toArray(new String[] {}),
        params.get("defaultCat"), 100).length);
      ClassifierResult result = classifier.classifyDocument(document.toArray(new String[] {}), params
          .get("defaultCat"));
      assertEquals(entry[0], result.getLabel());
      resultAnalyzer.addInstance(entry[0], result);
    }
    int[][] matrix = resultAnalyzer.getConfusionMatrix().getConfusionMatrix();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (i == j) assertEquals(4, matrix[i][j]);
        else assertEquals(0, matrix[i][j]);
      }
    }
    params.set("testDirPath", "testdata/bayesinput");
    TestClassifier.classifyParallel(params);
    Configuration conf = new Configuration();
    Path outputFiles = new Path("testdata/bayesinput-output/part*");
    FileSystem fs = FileSystem.get(outputFiles.toUri(), conf);
    matrix = BayesClassifierDriver.readResult(fs, outputFiles, conf, params).getConfusionMatrix();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (i == j) assertEquals(4, matrix[i][j]);
        else assertEquals(0, matrix[i][j]);
      }
    }
  }
  
  public void testSelfTestCBayes() throws InvalidDatastoreException, IOException {
    BayesParameters params = new BayesParameters(1);
    params.set("alpha_i", "1.0");
    params.set("dataSource", "hdfs");
    TrainClassifier.trainCNaiveBayes("testdata/bayesinput", "testdata/cbayesmodel", params);
    
    params.set("verbose", "true");
    params.set("basePath", "testdata/cbayesmodel");
    params.set("classifierType", "cbayes");
    params.set("dataSource", "hdfs");
    params.set("defaultCat", "unknown");
    params.set("encoding", "UTF-8");
    params.set("alpha_i", "1.0");
    
    Algorithm algorithm = new CBayesAlgorithm();
    Datastore datastore = new InMemoryBayesDatastore(params);
    ClassifierContext classifier = new ClassifierContext(algorithm, datastore);
    classifier.initialize();
    ResultAnalyzer resultAnalyzer = new ResultAnalyzer(classifier.getLabels(), params.get("defaultCat"));
    for (String[] entry : ClassifierData.DATA) {
      List<String> document = new NGrams(entry[1], Integer.parseInt(params.get("gramSize")))
          .generateNGramsWithoutLabel();
      assertEquals(3, classifier.classifyDocument(document.toArray(new String[] {}),
        params.get("defaultCat"), 100).length);
      ClassifierResult result = classifier.classifyDocument(document.toArray(new String[] {}), params
          .get("defaultCat"));
      assertEquals(entry[0], result.getLabel());
      resultAnalyzer.addInstance(entry[0], result);
    }
    int[][] matrix = resultAnalyzer.getConfusionMatrix().getConfusionMatrix();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (i == j) assertEquals(4, matrix[i][j]);
        else assertEquals(0, matrix[i][j]);
      }
    }
    params.set("testDirPath", "testdata/bayesinput");
    TestClassifier.classifyParallel(params);
    Configuration conf = new Configuration();
    Path outputFiles = new Path("testdata/bayesinput-output/part*");
    FileSystem fs = FileSystem.get(outputFiles.toUri(), conf);
    matrix = BayesClassifierDriver.readResult(fs, outputFiles, conf, params).getConfusionMatrix();
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (i == j) assertEquals(4, matrix[i][j]);
        else assertEquals(0, matrix[i][j]);
      }
    }
  }
  
}
