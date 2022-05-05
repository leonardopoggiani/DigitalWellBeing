#### Datasets

The `DataSource` class is not limited to ARFF files. It can also read CSV files and other formats (basically all file formats that Weka can import via its converters; it uses the file extension to determine the associated loader).

```java
 import weka.core.converters.ConverterUtils.DataSource;
 ...
 DataSource source = new DataSource("/some/where/data.arff");
 Instances data = source.getDataSet();
 // setting class attribute if the data format does not provide this information
 // For example, the XRFF format saves the class attribute information as well
 if (data.classIndex() == -1)
   data.setClassIndex(data.numAttributes() - 1);
```

#### Filtering on-the-fly

The [FilteredClassifer](https://weka.sourceforge.io/doc.stable-3-8/weka/classifiers/meta/FilteredClassifier.html) meta-classifier is an easy way of filtering data on the fly. It removes the necessity of filtering the data before the classifier can be trained. Also, the data need not be passed through the trained filter again at prediction time. The following is an example of using this meta-classifier with the `Remove` filter and `J48` for getting rid of a numeric ID attribute in the data:

```java
 import weka.classifiers.meta.FilteredClassifier;
 import weka.classifiers.trees.J48;
 import weka.filters.unsupervised.attribute.Remove;
 ...
 Instances train = ...         // from somewhere
 Instances test = ...          // from somewhere
 // filter
 Remove rm = new Remove();
 rm.setAttributeIndices("1");  // remove 1st attribute
 // classifier
 J48 j48 = new J48();
 j48.setUnpruned(true);        // using an unpruned J48
 // meta-classifier
 FilteredClassifier fc = new FilteredClassifier();
 fc.setFilter(rm);
 fc.setClassifier(j48);
 // train and make predictions
 fc.buildClassifier(train);
 for (int i = 0; i < test.numInstances(); i++) {
   double pred = fc.classifyInstance(test.instance(i));
   System.out.print("ID: " + test.instance(i).value(0));
   System.out.print(", actual: " + test.classAttribute().value((int) test.instance(i).classValue()));
   System.out.println(", predicted: " + test.classAttribute().value((int) pred));
 }
```

##### Train/test set

In case you have a dedicated test set, you can train the classifier and then evaluate it on this test set. In the following example, a J48 is instantiated, trained and then evaluated. Some statistics are printed to `stdout`:

```java
 import weka.core.Instances;
 import weka.classifiers.Evaluation;
 import weka.classifiers.trees.J48;
 ...
 Instances train = ...   // from somewhere
 Instances test = ...    // from somewhere
 // train classifier
 Classifier cls = new J48();
 cls.buildClassifier(train);
 // evaluate classifier and print some statistics
 Evaluation eval = new Evaluation(train);
 eval.evaluateModel(cls, test);
 System.out.println(eval.toSummaryString("\nResults\n======\n", false));
```

#### Classifying instances

In case you have an unlabeled dataset that you want to classify with your newly trained classifier, you can use the following code snippet. It loads the file `/some/where/unlabeled.arff`, uses the previously built classifier `tree` to label the instances, and saves the labeled data as `/some/where/labeled.arff`.

```java
 import java.io.BufferedReader;
 import java.io.BufferedWriter;
 import java.io.FileReader;
 import java.io.FileWriter;
 import weka.core.Instances;
 ...
 // load unlabeled data
 Instances unlabeled = new Instances(
                         new BufferedReader(
                           new FileReader("/some/where/unlabeled.arff")));

 // set class attribute
 unlabeled.setClassIndex(unlabeled.numAttributes() - 1);

 // create copy
 Instances labeled = new Instances(unlabeled);

 // label instances
 for (int i = 0; i < unlabeled.numInstances(); i++) {
   double clsLabel = tree.classifyInstance(unlabeled.instance(i));
   labeled.instance(i).setClassValue(clsLabel);
 }
 // save labeled data
 BufferedWriter writer = new BufferedWriter(
                           new FileWriter("/some/where/labeled.arff"));
 writer.write(labeled.toString());
 writer.newLine();
 writer.flush();
 writer.close();
```