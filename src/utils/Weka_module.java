/*
 * Weka
 */
package utils;

import biodiscml.Main;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;
import libsvm.*;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.apache.commons.math3.exception.MaxCountExceededException;
import static utils.utils.getMean;
import weka.attributeSelection.AttributeSelection;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.evaluation.ThresholdCurve;
import weka.classifiers.evaluation.output.prediction.PlainText;
import weka.core.Attribute;
import weka.core.Instances;
import weka.core.Range;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.Utils;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.supervised.instance.StratifiedRemoveFolds;
import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.filters.unsupervised.instance.RemoveRange;

/**
 *
 * @author Mickael
 */
public class Weka_module {

    public DecimalFormat df = new DecimalFormat();
    public ArrayList<String> alPrint = new ArrayList<>();
    private File CSVFile;
    public String ARFFfile;
    public boolean debug = true;
    public Instances myData;

    public Weka_module() {
        df.setMaximumFractionDigits(3);
        DecimalFormatSymbols dec = new DecimalFormatSymbols();
        dec.setDecimalSeparator('.');
        df.setGroupingUsed(false);
        df.setDecimalFormatSymbols(dec);
//        svm.svm_set_print_string_function(new libsvm.svm_print_interface() {
//            @Override
//            public void print(String s) {
//            } // Disables svm output
//        });

    }

    /**
     * keyword to search in attribute names to remove them
     *
     * @param keyword
     * @param attributeSelection
     */
    public void csvToArffRegressionWithFilter(String keyword, boolean attributeSelection) {
        try {
            //load csv
            if (debug) {
                System.out.println("loading csv");
            }
            CSVLoader csv = new CSVLoader();
            csv.setSource(CSVFile);
            csv.setMissingValue("?");
            //csv.setFieldSeparator("\t");
            Instances data = csv.getDataSet();

            //remove data having the keyword
            if (debug) {
                System.out.println("cleaning csv");
            }
            String[] opts = new String[2];
            opts[0] = "-R";
            String indexes = "";
            for (int i = 0; i < data.numAttributes(); i++) {
                if (data.attribute(i).name().contains(keyword)) {
                    //System.out.println("removing " + data.attribute(i).name());
                    indexes += (i + 1) + ",";
                }
            }
            opts[1] = indexes;
            weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
            r.setOptions(opts);
            r.setInputFormat(data);
            data = Filter.useFilter(data, r);

            if (debug) {
                System.out.println("saving full arff");
            }
            ArffSaver arff = new ArffSaver();
            arff.setInstances(data);
            ARFFfile = CSVFile.toString().replace(".csv", ".arff");
            arff.setFile(new File(ARFFfile));
            arff.writeBatch();

            //filter out useless data
            if (debug) {
                System.out.println("attribute selection");
            }
            ArffLoader arf = new ArffLoader();
            arf.setSource(new File(ARFFfile));
            data = arf.getDataSet();

            if (attributeSelection) {
                //weka.filters.supervised.attribute.AttributeSelection -E "weka.attributeSelection.CfsSubsetEval " -S "weka.attributeSelection.BestFirst -D 1 -N 5"
                weka.filters.supervised.attribute.AttributeSelection select = new weka.filters.supervised.attribute.AttributeSelection();
                String options = "-E "
                        + "\"weka.attributeSelection.CfsSubsetEval \" -S \"weka.attributeSelection.BestFirst -D 1 -N 5\"";
                select.setOptions(weka.core.Utils.splitOptions((options)));
                select.setInputFormat(data);
                data = Filter.useFilter(data, select);
            }

            //save csv to arff
            if (debug) {
                System.out.println("saving CfsSubsetEval Best Firsts arff");
            }
            arff = new ArffSaver();
            arff.setInstances(data);
            ARFFfile = CSVFile.toString().replace(".csv", ".CfsSubset_BestFirst.arff");
            arff.setFile(new File(ARFFfile));
            arff.writeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void csvToArffRegression() {
        if (Main.debug) {
            System.out.println("\n\nConvert " + CSVFile + " to arff");
        }
        File f = new File(CSVFile.toString().replace(".csv", ".arff"));
        f.delete();
        try {
            //load csv
            CSVLoader csv = new CSVLoader();
            csv.setSource(CSVFile);
            csv.setMissingValue("?");
            //csv.setFieldSeparator("\t");
            Instances data = csv.getDataSet();

            //save csv to arff
            ArffSaver arff = new ArffSaver();
            arff.setInstances(data);
            ARFFfile = CSVFile.toString().replace(".csv", ".arff");
            arff.setFile(new File(ARFFfile));
            arff.writeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void csvToArff(boolean classification) {
        if (Main.debug) {
            System.out.println("Convert " + CSVFile + " to arff");
        }
        File f = new File(CSVFile.toString().replace(".csv", ".arff"));
        f.delete();
        try {
            //load csv
            CSVLoader csv = new CSVLoader();
            csv.setSource(CSVFile);
            csv.setNominalAttributes("1"); //change ID to nominal
            csv.setMissingValue("?");
            String separator = utils.detectSeparator(CSVFile.toString());
            csv.setFieldSeparator(separator);
            Instances data = csv.getDataSet();

            //change numeric to nominal class
            if (classification) {
                weka.filters.unsupervised.attribute.NumericToNominal n
                        = new weka.filters.unsupervised.attribute.NumericToNominal();
                String[] opts = new String[2];
                opts[0] = "-R";
                opts[1] = "last";
                n.setOptions(opts);
                n.setInputFormat(data);
                data = Filter.useFilter(data, n);
                if (data.classIndex() == -1) {
                    data.setClassIndex(data.numAttributes() - 1);
                }

                //class order
                if (data.classAttribute().enumerateValues().hasMoreElements()) {
                    if (data.classAttribute().enumerateValues().nextElement().equals("false")
                            || data.classAttribute().enumerateValues().nextElement().equals("0")) {
                        weka.filters.unsupervised.attribute.SwapValues s = new weka.filters.unsupervised.attribute.SwapValues();
                        s.setOptions(weka.core.Utils.splitOptions(("-C last -F first -S last")));
                        s.setInputFormat(data);
                        data = Filter.useFilter(data, s);
                    }
                }
            }

            //save csv to arff
            ArffSaver arff = new ArffSaver();
            arff.setInstances(data);
            ARFFfile = CSVFile.toString().replace(".csv", ".arff");
            arff.setFile(new File(ARFFfile));
            arff.writeBatch();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> trainRegression() {
        ArrayList<String> al = new ArrayList<>();

        try {
            // load data
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(ARFFfile);
            Instances data = source.getDataSet();

            //set last attribute as index
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            // train model
            ////weka.classifiers.functions.GaussianProcesses
//            weka.classifiers.functions.GaussianProcesses model = new weka.classifiers.functions.GaussianProcesses();
//            String options = "-L 1.0 -N 0 -K \"weka.classifiers.functions.supportVector.NormalizedPolyKernel -C 250007 -E 2.0\"";
//            model.setOptions(weka.core.Utils.splitOptions((options)));
            ////weka.classifiers.functions.SMOreg
            weka.classifiers.functions.SMOreg model = new weka.classifiers.functions.SMOreg();
            String options = "-C 1.0 -N 0 -I \"weka.classifiers.functions.supportVector.RegSMO -L 0.001 -W 1 -P 1.0E-12\" -K \"weka.classifiers.functions.supportVector.NormalizedPolyKernel -C 250007 -E 2.0\"";
            model.setOptions(weka.core.Utils.splitOptions((options)));

            // cross validation 10 times on the model
            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(model, data, 10, new Random(1));

            //output classification results
            //System.out.println(eval.toSummaryString());
            al.add(Utils.doubleToString(eval.correlationCoefficient(), 12, 4));
            al.add(Utils.doubleToString(eval.meanAbsoluteError(), 12, 4));
            al.add(Utils.doubleToString(eval.rootMeanSquaredError(), 12, 4));
            al.add(Utils.doubleToString(eval.relativeAbsoluteError(), 12, 4));
            al.add(Utils.doubleToString(eval.rootRelativeSquaredError(), 12, 4));
            al.add("" + (data.numAttributes() - 1)); // number of genes

        } catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    /**
     * http://bayesianconspiracy.blogspot.ca/2009/10/usr-bin-env-groovy-import-weka_10.html
     *
     * @param attributesToUse
     * @return
     */
    public Object trainClassifier(String classifier,
            String classifier_options,
            String attributesToUse,
            Boolean classification, int numberOfFolds) {
        try {
            // load data
            Instances data = myData;
            //set last attribute as index
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            /**
             * if we are using Vote, it means it already contains the features
             * to use for each classifier
             */
            String configuration = classifier + " " + classifier_options;
            if (!classifier.contains("meta.Vote")) {
                //keep only attributesToUse. ID is still here
                String options = "";
                weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
                options = "-V -R " + attributesToUse;
                r.setOptions(weka.core.Utils.splitOptions((options)));
                r.setInputFormat(data);
                data = Filter.useFilter(data, r);
                data.setClassIndex(data.numAttributes() - 1);

                //create weka configuration string
                //remove ID from classification
                String filterID = ""
                        + "weka.classifiers.meta.FilteredClassifier "
                        + "-F \"weka.filters.unsupervised.attribute.Remove -R 1\" "
                        + "-W weka.classifiers.";

                //create config
                configuration = filterID + "" + classifier + " -- " + classifier_options;
            }

            //if cost sensitive case
            if (classifier.contains("CostSensitiveClassifier")) {
                int falseExemples = data.attributeStats(data.classIndex()).nominalCounts[1];
                int trueExemples = data.attributeStats(data.classIndex()).nominalCounts[0];
                double ratio = (double) trueExemples / (double) falseExemples;
                classifier_options = classifier_options.replace("ratio", df.format(ratio));
            }

            //train
            final String config[] = Utils.splitOptions(configuration);
            String classname = config[0];
            config[0] = "";
            Classifier model = null;

            model = (Classifier) Utils.forName(Classifier.class, classname, config);

            // evaluation
            Evaluation eval = new Evaluation(data);
            StringBuffer sb = new StringBuffer();
            PlainText pt = new PlainText();
            pt.setBuffer(sb);

            //10 fold cross validation
            eval.crossValidateModel(model, data, numberOfFolds, new Random(1), pt, new Range("first,last"), true);

            //build model to save it
            model.buildClassifier(data);
            // System.out.println(model.toString().split("\n")[0]);
            //return
            if (classification) {
                return new ClassificationResultsObject(eval, sb, data, model);
            } else {
                return new RegressionResultsObject(eval, sb, data, model);
            }

        } catch (Exception e) {
            String message = "[model error] " + classifier + " " + classifier_options + " | " + e.getMessage();
            if (Main.debug) {
//                if (!e.getMessage().contains("handle") && !e.getMessage().contains("supported")) {
//
//                }
                e.printStackTrace();
                System.err.println(message);

            }
            return message;

        }

    }

    public Object trainClassifierHoldOutCVandTest(String classifier, String classifier_options,
            String attributesToUse, Boolean classification, int seed) {
        try {
            // load data
            Instances data = myData;

            //set class index
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            String configuration = classifier + " " + classifier_options;
            if (!classifier.contains("meta.Vote") && attributesToUse != null) {
                //keep only attributesToUse. ID (if present) is still here
                String options = "";
                weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
                options = "-V -R " + attributesToUse;
                r.setOptions(weka.core.Utils.splitOptions((options)));
                r.setInputFormat(data);
                data = Filter.useFilter(data, r);
                data.setClassIndex(data.numAttributes() - 1);

                //create weka configuration string
                String filterID = "weka.classifiers.meta.FilteredClassifier "
                        + "-F \"weka.filters.unsupervised.attribute.Remove -R 1\" "
                        + "-W weka.classifiers.";
                configuration = filterID + "" + classifier + " -- " + classifier_options;
            }

            // randomize data with number generator
            data.randomize(new Random(seed));

            // Percent split
            int trainSize = (int) Math.round(data.numInstances() * 66 / 100);
            int testSize = data.numInstances() - trainSize;
            Instances train = new Instances(data, 0, trainSize);
            Instances test = new Instances(data, trainSize, testSize); //set last attribute as index

            //if cost sensitive case
            if (classifier.contains("CostSensitiveClassifier")) {
                int falseExemples = train.attributeStats(train.classIndex()).nominalCounts[1];
                int trueExemples = train.attributeStats(train.classIndex()).nominalCounts[0];
                double ratio = (double) trueExemples / (double) falseExemples;
                classifier_options = classifier_options.replace("ratio", df.format(ratio));
            }

            //train
            String config[] = Utils.splitOptions(configuration);
            String classname = config[0];
            config[0] = "";
            Classifier model = (Classifier) Utils.forName(Classifier.class, classname, config);
            StringBuffer sb = new StringBuffer();

            //build model to save it
            model.buildClassifier(train);

            //evaluation
            Evaluation eval = new Evaluation(test);
            eval.evaluateModel(model, test);

            //return
            if (classification) {
                return new ClassificationResultsObject(eval, sb, data, model);
            } else {
                return new RegressionResultsObject(eval, sb, data, model);
            }
        } catch (Exception e) {
            if (Main.debug) {
                e.printStackTrace();
            }
            //System.err.println(e.getMessage() + " for " + classifier + " " + classifier_options);
            return null;
        }

    }

    public void attributeSelectionByRelieFFAndSaveToCSV(String outfile) {

        // load data
        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(ARFFfile);
            Instances data = source.getDataSet();
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            //store IDs
//            ArrayList<String> alIDs = new ArrayList<String>();
//            String IDname = data.attribute(0).name();
//            FastVector fv = new FastVector();
//            Enumeration en = data.attribute(0).enumerateValues();
//            while (en.hasMoreElements()) {
//                fv.addElement((String) en.nextElement());
//            }
//            data.deleteAttributeAt(0);
            weka.filters.supervised.attribute.AttributeSelection select = new weka.filters.supervised.attribute.AttributeSelection();

            //filter based on ranker score threshold
            String options = "-E \"weka.attributeSelection.ReliefFAttributeEval -M -1 -D 1 -K 10\" -S \"weka.attributeSelection.Ranker -T -1.79E308 -N -1\"";
            select.setOptions(weka.core.Utils.splitOptions((options)));
            select.setInputFormat(data);
            Instances filteredData = Filter.useFilter(data, select);

            System.out.println("Total attributes: " + (filteredData.numAttributes() - 1));
            //remove some attribute if we have too many of them
            if (filteredData.numAttributes() > Main.maxNumberOfSelectedFeatures) {
                System.out.println("Too many attributes, only keep best " + Main.maxNumberOfSelectedFeatures);
                String limit = " -N " + Main.maxNumberOfSelectedFeatures;
                options = "-E \"weka.attributeSelection.ReliefFAttributeEval -M -1 -D 1 -K 10\" -S \"weka.attributeSelection.Ranker -T 0.01" + limit + "\"";
                select.setOptions(weka.core.Utils.splitOptions((options)));
                select.setInputFormat(filteredData);
                filteredData = Filter.useFilter(filteredData, select);

            } else if (filteredData.numAttributes() == 0) {
                System.out.println("Not enough attributes, probably all non-informative. So keeping all and ranking them, but expect low training performance");
                options = "-E \"weka.attributeSelection.ReliefFAttributeEval -M -1 -D 1 -K 10\" -S \"weka.attributeSelection.Ranker -T -1 -N -1\"";
                select.setOptions(weka.core.Utils.splitOptions((options)));
                select.setInputFormat(data);
                filteredData = Filter.useFilter(data, select);
            }

//            //restore IDs
//            Attribute att = new Attribute(IDname, fv);
//            filteredData.insertAttributeAt(att, 0);
//            for (int i = 0; i < filteredData.numInstances(); i++) {
//                filteredData.instance(i).setValue(0, (String) fv.elementAt(i));
//            }
//            filteredData.instance(1);
//            filteredData.attribute(1);
            //save data as arff
            CSVSaver csv = new CSVSaver();
            csv.setInstances(filteredData);
            csv.setFile(new File(outfile));
            if (new File(outfile.replace(".csv", ".arff")).exists()) {
                new File(outfile.replace(".csv", ".arff")).delete();
            }
            csv.writeBatch();
            myData = filteredData;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void attributeSelectionByInfoGainRankingAndSaveToCSV(String outfile) {
        // load data

        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(ARFFfile);
            Instances data = source.getDataSet();
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            //delete ids
            //data.deleteAttributeAt(0);

            weka.filters.supervised.attribute.AttributeSelection select = new weka.filters.supervised.attribute.AttributeSelection();
            String options = "-E "
                    + "\"weka.attributeSelection.InfoGainAttributeEval \" -S \"weka.attributeSelection.Ranker -T 0.001 -N -1\"";
            select.setOptions(weka.core.Utils.splitOptions((options)));
            select.setInputFormat(data);

            Instances filteredData = Filter.useFilter(data, select);

            System.out.println("Total attributes: " + (filteredData.numAttributes() - 1));
            //remove some attribute if we have too many of them
            if (filteredData.numAttributes() > Main.maxNumberOfSelectedFeatures) {
                System.out.println("Too many attributes, only keep best " + Main.maxNumberOfSelectedFeatures);
                String limit = " -N " + Main.maxNumberOfSelectedFeatures;
                options = "-E \"weka.attributeSelection.InfoGainAttributeEval \" -S \"weka.attributeSelection.Ranker -T 0.001" + limit + "\"";
                select.setOptions(weka.core.Utils.splitOptions((options)));
                select.setInputFormat(filteredData);
                filteredData = Filter.useFilter(filteredData, select);

            } else if (filteredData.numAttributes() <= 10) {
                //System.out.println("Not enough attributes, probably all non-informative. So keeping all and ranking them, but expect very low performances");
                options = "-E \"weka.attributeSelection.InfoGainAttributeEval \" -S \"weka.attributeSelection.Ranker -T -1 -N -1\"";
                select.setOptions(weka.core.Utils.splitOptions((options)));
                select.setInputFormat(data);
                filteredData = Filter.useFilter(data, select);
            }

            //save data as csv
            CSVSaver csv = new CSVSaver();
            csv.setInstances(filteredData);
            csv.setFile(new File(outfile));
            if (new File(outfile.replace(".csv", ".arff")).exists()) {
                new File(outfile.replace(".csv", ".arff")).delete();
            }
            csv.writeBatch();
            myData = filteredData;

            //evaluation.crossValidateModel(classifier, data, 10, new Random(1));
        } catch (Exception e) {
            System.err.println(e.getMessage() + "\nImpossible to execute InfoGain. Trying directly Ranking");
            featureRankingForClassification();
        }
    }

    public void attributeSelection(String evaluator, String outfileExtension) {
        // load data
        try {
            Instances data = myData;
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
//            //remove patient ID
//            if (myData.attribute(0).name().toLowerCase().contains("patient")) {
//                data.deleteAttributeAt(0);
//            }

            weka.filters.supervised.attribute.AttributeSelection select = new weka.filters.supervised.attribute.AttributeSelection();
            select.setOptions(weka.core.Utils.splitOptions((evaluator)));
            select.setInputFormat(data);
            data = Filter.useFilter(data, select);

            //save data as arff
            ArffSaver arffsav = new ArffSaver();
            arffsav.setInstances(data);
            ARFFfile = ARFFfile.replace(".arff", "." + outfileExtension + ".arff");
            arffsav.setFile(new File(ARFFfile));
            arffsav.writeBatch();

            //evaluation.crossValidateModel(classifier, data, 10, new Random(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AttributeSelection featureRankingForClassification() {
        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(ARFFfile);
            Instances data = source.getDataSet();
            AttributeSelection attrsel = new AttributeSelection();
            weka.attributeSelection.InfoGainAttributeEval eval = new weka.attributeSelection.InfoGainAttributeEval();

            weka.attributeSelection.Ranker rank = new weka.attributeSelection.Ranker();
            rank.setOptions(
                    weka.core.Utils.splitOptions(
                            "-T 0.001 -N -1"));
            attrsel.setEvaluator(eval);
            attrsel.setSearch(rank);

            attrsel.SelectAttributes(data);
            return attrsel;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Check your input files. Probably missing classes");
            System.exit(0);
            return null;
        }
    }

    /**
     *
     * @param csvFile
     * @return ranking output
     */
    public String featureRankingForClassification(String csvFile) {
        try {
            //csvToArff(true);
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(csvFile);
            Instances data = source.getDataSet();
            AttributeSelection attrsel = new AttributeSelection();
            weka.attributeSelection.InfoGainAttributeEval eval = new weka.attributeSelection.InfoGainAttributeEval();

            weka.attributeSelection.Ranker rank = new weka.attributeSelection.Ranker();
            rank.setOptions(
                    weka.core.Utils.splitOptions(
                            "-T 0.001 -N -1"));
            attrsel.setEvaluator(eval);
            attrsel.setSearch(rank);

            try {
                attrsel.SelectAttributes(data);
            } catch (UnsupportedAttributeTypeException e) {
                //in case of id seen as string instead of nominal
                weka.filters.unsupervised.attribute.StringToNominal f = new StringToNominal();
                String options = "-R first";
                f.setOptions(weka.core.Utils.splitOptions((options)));
                f.setInputFormat(data);
                data = Filter.useFilter(data, f);
                attrsel.SelectAttributes(data);
            }
            return attrsel.toResultsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Can't compute feature ranking";
        }
    }

    public AttributeSelection featureRankingForRegression() {
        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(ARFFfile);
            Instances data = source.getDataSet();
            AttributeSelection attrsel = new AttributeSelection();
            weka.attributeSelection.CfsSubsetEval eval = new weka.attributeSelection.CfsSubsetEval();
            weka.attributeSelection.GreedyStepwise rank = new weka.attributeSelection.GreedyStepwise();
            rank.setOptions(
                    weka.core.Utils.splitOptions(
                            "-R -T -1.7976931348623157E308 -N -1"));

            attrsel.setEvaluator(eval);
            attrsel.setSearch(rank);

            attrsel.SelectAttributes(data);
            return attrsel;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String featureRankingForRegression(String csvFile) {
        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(csvFile);
            Instances data = source.getDataSet();
            AttributeSelection attrsel = new AttributeSelection();

            weka.attributeSelection.ReliefFAttributeEval eval = new weka.attributeSelection.ReliefFAttributeEval();
            weka.attributeSelection.Ranker rank = new weka.attributeSelection.Ranker();
            rank.setOptions(
                    weka.core.Utils.splitOptions(
                            "-T -1.7976931348623157E308 -N -1"));

            attrsel.setEvaluator(eval);
            attrsel.setSearch(rank);

            attrsel.SelectAttributes(data);
            return attrsel.toResultsString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public StringBuffer makePredictions(File testarff, String classifier) {
        System.out.println("test file " + testarff.toString() + " on model " + classifier);
        StringBuffer sb = new StringBuffer();

        StringBuilder sbIds = new StringBuilder();
        try {
            // load data
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(testarff.toString());
            Instances data = source.getDataSet();
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            Classifier model = (Classifier) weka.core.SerializationHelper.read(classifier);

            //get class names
            HashMap<Integer, String> hmClassCorrespondance = new HashMap<>();
            for (String s : model.toString().split("\n")) {
                if (s.startsWith("@attribute class")) {
                    s = s.split("\\{")[1].replace("}", "");
                    String classes[] = s.split(",");
                    for (int i = 0; i < classes.length; i++) {
                        hmClassCorrespondance.put(i, classes[i]);
                    }
                }
            }
            // label instances
            sb.append("instance\tprediction\tprobability\n");
            for (int i = 0; i < data.numInstances(); i++) {
                double prediction = model.classifyInstance(data.instance(i));
                double probability = model.distributionForInstance(data.instance(i))[(int) prediction];

                sb.append(data.attribute(0).value(i)).
                        append("\t").append(hmClassCorrespondance.get((int) prediction)).
                        append("\t").append(probability).
                        append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb;
    }

    /**
     * test a classifier
     *
     * @param testarff
     * @param classifier file
     * @param classification
     * @return
     */
    public Object testClassifierFromFileSource(File testarff, String classifier, boolean classification) {
        if (Main.debug) {
            System.out.println("test file " + testarff.toString() + " on model " + classifier);
        }
        StringBuffer sb = new StringBuffer();
        try {
            // load data
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(testarff.toString());
            Instances data = source.getDataSet();
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }
            //load model
            Classifier model = (Classifier) weka.core.SerializationHelper.read(classifier);

            // evaluate dataset on the model
            Evaluation eval = new Evaluation(data);
            PlainText pt = new PlainText();
            pt.setBuffer(sb);

            //eval.evaluateModel(model, data, pt, new Range("first,last"), true);
            eval.evaluateModel(model, data);

            //return
            if (classification) {
                return new ClassificationResultsObject(eval, sb, data, model);
            } else {
                return new RegressionResultsObject(eval, sb, data, model);
            }

        } catch (Exception e) {
            if (Main.debug) {
                e.printStackTrace();
            }
        }
        return null;

    }

    /**
     * test a classifier
     *
     * @param testarff
     * @param classifier file
     * @param classification
     * @return
     */
    public Object testClassifierFromModel(Classifier model, String testFile, boolean classification, String out) {

        StringBuffer sb = new StringBuffer();
        try {
            // load data
            ConverterUtils.DataSource testSource = new ConverterUtils.DataSource(testFile.replace("csv", "arff"));
            Instances testData = testSource.getDataSet();

            testData = extractFeaturesFromDatasetBasedOnModel(model, testData);
            testData.setClassIndex(testData.numAttributes() - 1);
            //remove ID
            //testData.deleteAttributeAt(0);

            // evaluate test dataset on the model
            Evaluation eval = new Evaluation(testData);
            PlainText pt = new PlainText();
            pt.setHeader(testData);
            pt.setBuffer(sb);
            eval.evaluateModel(model, testData, pt, new Range("first,last"), true);

//            //saving for debug
//            SerializationHelper.write(Main.wd + "tmp." + out.replace("\t", "_") + ".model", model);
//            ArffSaver arff = new ArffSaver();
//            arff.setInstances(testData);
//            arff.setFile(new File(Main.wd + "tmp." + out.replace("\t", "_") + ".testdata.arff"));
//            arff.writeBatch();
            //return
            if (classification) {
                return new ClassificationResultsObject(eval, sb, testData, model);
            } else {
                return new RegressionResultsObject(eval, sb, testData, model);
            }

        } catch (Exception e) {
            if (Main.debug) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }

        }
        return null;

    }

//    /**
//     * test a classifier
//     *
//     * @param testarff
//     * @param classifier file
//     * @param classification
//     * @return
//     */
//    public Object testClassifier(String classifier, String classifier_options, String features, String trainFile, String testFile, boolean classification) {
//
//        StringBuffer sb = new StringBuffer();
//        try {
//            // load data
//            ConverterUtils.DataSource trainSource = new ConverterUtils.DataSource(trainFile.replace("csv", "arff"));
//            ConverterUtils.DataSource testSource = new ConverterUtils.DataSource(testFile.replace("csv", "arff"));
//
//            Instances trainData = trainSource.getDataSet();
//            trainData.setClassIndex(trainData.numAttributes() - 1);
//            Instances testData = testSource.getDataSet();
//            testData.setClassIndex(testData.numAttributes() - 1);
//
//            //load model
//            String configuration = classifier + " " + classifier_options;
//            if (!classifier.contains("meta.Vote")) {
//                //keep only attributesToUse. ID is still here
//                String options = "";
//                weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
//                options = "-V -R " + features;
//                r.setOptions(weka.core.Utils.splitOptions((options)));
//                r.setInputFormat(trainData);
//                trainData = Filter.useFilter(trainData, r);
//                trainData.setClassIndex(trainData.numAttributes() - 1);
//                testData = Filter.useFilter(testData, r);
//                testData.setClassIndex(testData.numAttributes() - 1);
//
//                //create weka configuration string
//                //remove ID from classification
//                String filterID = ""
//                        + "weka.classifiers.meta.FilteredClassifier "
//                        + "-F \"weka.filters.unsupervised.attribute.Remove -R 1\" "
//                        + "-W weka.classifiers.";
//
//                //create config
//                configuration = filterID + "" + classifier + " -- " + classifier_options;
//
//            }
//
//            //create model
//            final String config[] = Utils.splitOptions(configuration);
//            String classname = config[0];
//            config[0] = "";
//            Classifier model = null;
//
//            model = (Classifier) Utils.forName(Classifier.class, classname, config);
//
//            // evaluate dataset on the model
//            Evaluation eval = new Evaluation(testData);
//            PlainText pt = new PlainText();
//            pt.setBuffer(sb);
//
//            //1 fold cross validation
//            eval.crossValidateModel(model, trainData, 10, new Random(1), pt, new Range("first,last"), true);
//
//            eval.evaluateModel(model, testData);
//            model.buildClassifier(trainData);
//
//            //return
//            if (classification) {
//                return new ClassificationResultsObject(eval, sb, testData, model);
//            } else {
//                return new RegressionResultsObject(eval, sb, testData, model);
//            }
//
//        } catch (Exception e) {
//            if (Main.debug) {
//                e.printStackTrace();
//            }
//        }
//        return null;
//
//    }
    public void setCSVFile(File file) {
        CSVFile = file;
    }

    public void setARFFfile(String file) {
        ARFFfile = file;
    }

    /**
     * set local variable mydata with ARFFfile
     */
    public void setDataFromArff() {
        // load data
        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(ARFFfile);
            myData = source.getDataSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * set local variable mydata with CSVFfile. VERY DANGEROUS WITHOUT LOADER
     * THAT SET THE SEPARATOR Need to load as csvToArff()
     */
    public void setDataFromCSV() {
//        // load data
//        try {
//            //implement loader
//            ConverterUtils.DataSource source = new ConverterUtils.DataSource(CSVFile.toString());
//            myData = source.getDataSet();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    /**
     * TODO: find another way to detect classification vs regression. A user
     * could use 1 and 0 instead of true false;
     *
     * @return
     */
    public boolean isClassification() {
        try {
            Double.parseDouble(myData.instance(0).toString(myData.numAttributes() - 1));
            //Double.parseDouble(myData.attribute(myData.numAttributes() - 1).enumerateValues().nextElement().toString());
            return false;
        } catch (Exception e) {
            return true;
        }

    }

    public ArrayList<String> getFeaturesFromClassifier(String classifier) {
        //HashMap<String, String> hm = new HashMap<>();
        ArrayList<String> al = new ArrayList<>();
        try {
            //load model
            Classifier model = (Classifier) weka.core.SerializationHelper.read(classifier);
            for (String s : model.toString().split("\n")) {
                if (s.startsWith("@attribute")) {
                    //hm.put(s.split(" ")[1], s.split(" ")[2]); //feature_name, feature_type
                    al.add(s.split(" ")[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    public ArrayList<String> getFeaturesFromClassifier(Classifier model) {
        //HashMap<String, String> hm = new HashMap<>();
        ArrayList<String> al = new ArrayList<>();
        try {
            //load model
            for (String s : model.toString().split("\n")) {
                if (s.startsWith("@attribute")) {
                    //hm.put(s.split(" ")[1], s.split(" ")[2]); //feature_name, feature_type
                    al.add(s.split(" ")[1]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return al;
    }

    public void saveFilteredDataToArff(String attributesToUse, boolean classification, String outfile) {
        try {
            // load data
            Instances data = myData;

            //set class index
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            //keep only attributesToUse. ID is still here
            String options = "";
            weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
            options = "-V -R " + attributesToUse;
            r.setOptions(weka.core.Utils.splitOptions((options)));
            r.setInputFormat(data);
            data = Filter.useFilter(data, r);
            data.setClassIndex(data.numAttributes() - 1);

            //save csv to arff
            ArffSaver arff = new ArffSaver();
            arff.setInstances(data);
            arff.setFile(new File(outfile));
            arff.writeBatch();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void saveFilteredDataToCSV(String attributesToUse, boolean classification, String outfile) {
        try {
            // load data
            Instances data = myData;

            //set class index
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            //keep only attributesToUse. ID is still here
            String options = "";
            weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
            options = "-V -R " + attributesToUse;
            r.setOptions(weka.core.Utils.splitOptions((options)));
            r.setInputFormat(data);
            data = Filter.useFilter(data, r);
            data.setClassIndex(data.numAttributes() - 1);

            //save csv to arff
            CSVSaver csv = new CSVSaver();
            csv.setInstances(data);
            csv.setFile(new File(outfile));
            csv.writeBatch();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * perform sampling on the dataset
     *
     * @param fullDatasetFile
     * @param dataToTrainFile
     * @param dataToTestFile
     */
    public void sampling(String fullDatasetFile, String dataToTrainFile, String dataToTestFile, boolean classification, String range) {
        System.out.println("# sampling statistics");
        boolean useRange = !range.isEmpty();
        setCSVFile(new File(fullDatasetFile));
        csvToArff(classification);
        setDataFromArff();
        // load data
        Instances data = myData;
        //set class index
        if (data.classIndex() == -1) {
            data.setClassIndex(data.numAttributes() - 1);
        }
        //calculate seed
        Random rand = new Random();
        int seed = 1 + rand.nextInt(1000);

        try {
            Instances TestData = null;
            Instances TrainData = null;

            //create train set
            if (useRange) {
                weka.filters.unsupervised.instance.RemoveRange rr = new RemoveRange();
                String options = "-V -R " + range;
                rr.setOptions(weka.core.Utils.splitOptions((options)));
                rr.setInputFormat(data);
                TrainData = Filter.useFilter(data, rr);
            } else {
                weka.filters.supervised.instance.StratifiedRemoveFolds srf = new StratifiedRemoveFolds();
                String options = "-S " + seed + " -V -N " + Main.samplingFold + " -F 1"; //split dataset in 3 and inverse the selection
                srf.setOptions(weka.core.Utils.splitOptions((options)));
                srf.setInputFormat(data);
                TrainData = Filter.useFilter(data, srf);
            }
            System.out.println("Total number of instances in train data file: " + TrainData.numInstances());
            if (classification) {
                Enumeration e = TrainData.attribute(data.classIndex()).enumerateValues();
                int cpt = 0;
                while (e.hasMoreElements()) {
                    String className = (String) e.nextElement();
                    double n = TrainData.attributeStats(data.classIndex()).nominalWeights[cpt];
                    System.out.println("class " + className + ": " + n + " (" + df.format(n / (double) TrainData.numInstances() * 100) + "%)");
                    cpt++;
                }
            }

            //create test set
            if (useRange) {
                weka.filters.unsupervised.instance.RemoveRange rr = new RemoveRange();
                String options = "-R " + range;
                rr.setOptions(weka.core.Utils.splitOptions((options)));
                rr.setInputFormat(data);
                TestData = Filter.useFilter(data, rr);
            } else {
                weka.filters.supervised.instance.StratifiedRemoveFolds srf = new StratifiedRemoveFolds();
                String options = "-S " + seed + " -N " + Main.samplingFold + " -F 1"; //split dataset in 3
                srf.setOptions(weka.core.Utils.splitOptions((options)));
                srf.setInputFormat(data);
                TestData = Filter.useFilter(data, srf);
            }

            System.out.println("Total number of instances in test data file: " + TestData.numInstances());
            if (classification) {
                Enumeration e = TestData.attribute(data.classIndex()).enumerateValues();
                int cpt = 0;
                while (e.hasMoreElements()) {
                    String className = (String) e.nextElement();
                    double n = TestData.attributeStats(data.classIndex()).nominalWeights[cpt];
                    System.out.println("class " + className + ": " + n + " (" + df.format(n / (double) TestData.numInstances() * 100) + "%)");
                    cpt++;
                }
            }
            //save to csv and arff
            if (!useRange) {
                ///test set to csv
                CSVSaver csv = new CSVSaver();
                csv.setInstances(TestData);
                csv.setFile(new File(dataToTestFile));
                csv.writeBatch();
                ///train set to csv
                csv = new CSVSaver();
                csv.setInstances(TrainData);
                csv.setFile(new File(dataToTrainFile));
                csv.writeBatch();
            }

            //test set to arff
            ArffSaver arff = new ArffSaver();
            arff.setInstances(TestData);
            arff.setFile(new File(dataToTestFile.replace("csv", "arff")));
            arff.writeBatch();

            //train set to arff
            arff = new ArffSaver();
            arff.setInstances(TrainData);
            arff.setFile(new File(dataToTrainFile.replace("csv", "arff")));
            arff.writeBatch();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * in case of combined models using filters for selecting specific filters
     * based on indexes
     *
     * @param featureSelectedData
     * @param arffTestFileData
     * @param outfile
     */
    public void extractFeaturesFromTestFileBasedOnSelectedFeatures(Instances featureSelectedData, Instances arffTestFileData,
            String outfile) {
        //get attribute names of feature selection data
        ArrayList<String> aFeaturesSelected = new ArrayList<>();
        Enumeration<Attribute> enumerate = featureSelectedData.enumerateAttributes();
        while (enumerate.hasMoreElements()) {
            Attribute a = enumerate.nextElement();
            aFeaturesSelected.add(a.name());
        }
        HashMap<String, String> hmModelFeatures = new HashMap<>();//indexed hashed features
        for (String f : aFeaturesSelected) {
            hmModelFeatures.put(f, f);
        }

        Instances data = arffTestFileData;
        //get index of model features in the arff test file
        String indexesToKeep = "";
        Enumeration e = data.enumerateAttributes();
        while (e.hasMoreElements()) {
            Attribute a = (Attribute) e.nextElement();
            if (hmModelFeatures.containsKey(a.name())) {
                indexesToKeep += (a.index() + 1) + ",";
            }
        }
        indexesToKeep += myData.numAttributes();
        try {
            //apply remove filter
            String[] opts = new String[3];
            opts[0] = "-V";
            opts[1] = "-R";
            opts[2] = indexesToKeep;
            weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
            r.setOptions(opts);
            r.setInputFormat(data);
            data = Filter.useFilter(data, r);

            //reorder
            String order = ",";
            for (String featureOrdered : aFeaturesSelected) {
                int featureNotOrdered = data.attribute(featureOrdered).index() + 1;
                order += featureNotOrdered + ",";
            }
            order += data.attribute("class").index() + 1;

            Reorder reorder = new Reorder();
            reorder.setAttributeIndices(order);
            reorder.setInputFormat(data);
            data = Filter.useFilter(data, reorder);

            //save
            ArffSaver arff = new ArffSaver();
            arff.setInstances(data);
            arff.setFile(new File(outfile));
            arff.writeBatch();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * extract features from test file while keeping arff compatibility
     *
     * @param modelFile
     * @param arffTestFile
     * @param outfile
     */
    public void extractFeaturesFromTestFileBasedOnModel(String modelFile, Instances arffTestFile, String outfile) {
        Instances data = arffTestFile;
        //get model features
        ArrayList<String> alModelFeatures = getFeaturesFromClassifier(modelFile); //features in the right order
        HashMap<String, String> hmModelFeatures = new HashMap<>();//indexed hashed features
        for (String f : alModelFeatures) {
            hmModelFeatures.put(f, f);
        }

        //get index of model features in the arff test file
        String indexesToKeep = "1"; //keep ID
        Enumeration e = myData.enumerateAttributes();
        while (e.hasMoreElements()) {
            Attribute a = (Attribute) e.nextElement();
            if (hmModelFeatures.containsKey(a.name())) {
                indexesToKeep += "," + (a.index() + 1);
            }
        }
        if (!hmModelFeatures.containsKey("class")) {
            indexesToKeep += "," + myData.numAttributes();
        }
        try {
            //apply remove filter
            String[] opts = new String[3];
            opts[0] = "-V";
            opts[1] = "-R";
            opts[2] = indexesToKeep;
            weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
            r.setOptions(opts);
            r.setInputFormat(data);
            data = Filter.useFilter(data, r);

            //reorder
            String order = "1,";
            for (String featureOrdered : alModelFeatures) {
                int featureNotOrdered = data.attribute(featureOrdered).index() + 1;
                order += featureNotOrdered + ",";
            }
            Reorder reorder = new Reorder();
            reorder.setAttributeIndices(order);
            reorder.setInputFormat(data);
            data = Filter.useFilter(data, reorder);

            //save to arff
            ArffSaver arff = new ArffSaver();
            arff.setInstances(data);
            arff.setFile(new File(outfile + ".tmp"));
            arff.writeBatch();
            //change non-numeric attributes to ensure compatibility
            String classString = "";
            Classifier model = (Classifier) weka.core.SerializationHelper.read(modelFile);
            HashMap<String, String> hm = new HashMap<>();
            for (String s : model.toString().split("\n")) {
                if (s.startsWith("@attribute") && !s.endsWith(" numeric")) {
                    String attribute = s.replace("@attribute ", "");
                    attribute = attribute.substring(0, attribute.indexOf("{"));
                    hm.put(attribute, s);
                }
            }
            try {
                BufferedReader br = new BufferedReader(new FileReader(outfile + ".tmp"));
                PrintWriter pw = new PrintWriter(new FileWriter(outfile));
                while (br.ready()) {
                    String line = br.readLine();
                    if (line.startsWith("@attribute") && !line.endsWith(" numeric") && !line.contains("@attribute Instance")) {
                        String attribute = line.replace("@attribute ", "").trim();
                        attribute = attribute.substring(0, attribute.indexOf("{"));
                        pw.println(hm.get(attribute));
                    } else {
                        pw.println(line);
                    }
                }
                pw.close();
                br.close();
                new File(outfile + ".tmp").delete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * extract features from dataset
     *
     * @param modelFile
     * @param dataset
     * @param outfile
     */
    public Instances extractFeaturesFromDatasetBasedOnModel(Classifier model, Instances dataset) {
        //get model features
        ArrayList<String> alModelFeatures = getFeaturesFromClassifier(model); //features in the right order
        HashMap<String, String> hmModelFeatures = new HashMap<>();//indexed hashed features
        for (String f : alModelFeatures) {
            hmModelFeatures.put(f, f);
        }
        //get index of model features in the arff test file
        String indexesToKeep = "1"; //keep ID
        Enumeration e = dataset.enumerateAttributes();
        while (e.hasMoreElements()) {
            Attribute a = (Attribute) e.nextElement();
            if (hmModelFeatures.containsKey(a.name())) {
                indexesToKeep += "," + (a.index() + 1);
            }
        }
        if (!hmModelFeatures.containsKey("class")) {
            indexesToKeep += "," + dataset.numAttributes();
        }
        Instances filteredDataset = null;
        try {
            //apply remove filter
            String[] opts = new String[3];
            opts[0] = "-V";
            opts[1] = "-R";
            opts[2] = indexesToKeep;
            weka.filters.unsupervised.attribute.Remove r = new weka.filters.unsupervised.attribute.Remove();
            r.setOptions(opts);
            r.setInputFormat(dataset);
            filteredDataset = Filter.useFilter(dataset, r);

            //reorder
            String order = "1,";
            for (String featureOrdered : alModelFeatures) {
                int featureNotOrdered = filteredDataset.attribute(featureOrdered).index() + 1;
                order += featureNotOrdered + ",";
            }
            Reorder reorder = new Reorder();
            reorder.setAttributeIndices(order);
            reorder.setInputFormat(filteredDataset);
            filteredDataset = Filter.useFilter(filteredDataset, reorder);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return filteredDataset;

    }

    /**
     * ensure compatibility of non-numeric attributes between 2 arff files
     *
     * @param ArffFileWithReferenceHeader contains the header you want
     * @param ArffFileToModify file fo modify header
     */
    public void makeCompatibleARFFheaders(String ArffFileWithReferenceHeader, String ArffFileToModify) {
        HashMap<String, String> hm = new HashMap<>();
        try {
            // read source. Get non-numeric attributes
            BufferedReader br = new BufferedReader(new FileReader(ArffFileWithReferenceHeader));
            String line = "";
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("@attribute") && !line.trim().endsWith("numeric")) {
                    String attribute = line.replace("@attribute ", "");
                    attribute = attribute.substring(0, attribute.indexOf("{"));
                    hm.put(attribute, line);
                }
            }
            br.close();
            //read destination
            br = new BufferedReader(new FileReader(ArffFileToModify));
            PrintWriter pw = new PrintWriter(new FileWriter(ArffFileToModify + "_tmp"));
            while (br.ready()) {
                line = br.readLine();
                if (line.startsWith("@attribute") && !line.endsWith(" numeric")) {
                    String attribute = line.replace("@attribute ", "").trim();
                    attribute = attribute.substring(0, attribute.indexOf("{"));
                    pw.println(hm.get(attribute));
                } else {
                    pw.println(line);
                }
            }
            pw.close();
            br.close();
            new File(ArffFileToModify).delete();
            new File(ArffFileToModify + "_tmp").renameTo(new File(ArffFileToModify));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static class RegressionResultsObject {

        public String CC;//correlation coefficient
        public String MAE;//Mean absolute error
        public String RMSE;//Root mean squared error
        public String RAE;//Relative absolute error
        public String RRSE;//Root relative squared error
        public StringBuilder predictions = new StringBuilder();
        public StringBuilder features = new StringBuilder();
        public int numberOfFeatures = 0; //number of features
        public Classifier model;
        public String featuresRankingResults;
        public Evaluation evaluation;
        public Instances dataset;

        public RegressionResultsObject() {
        }

        private RegressionResultsObject(Evaluation eval, StringBuffer sb, Instances data, Classifier classifier) {
            dataset = data;
            evaluation = eval;

            try {
                parsePredictions(sb);
            } catch (Exception e) {
            }
            //getFeatures(data);
            getFeatures(classifier);
            model = classifier;

            //measures
            try {
                CC = Utils.doubleToString(eval.correlationCoefficient(), 12, 4).trim();
                MAE = Utils.doubleToString(eval.meanAbsoluteError(), 12, 4).trim();
                RMSE = Utils.doubleToString(eval.rootMeanSquaredError(), 12, 4).trim();
                RAE = Utils.doubleToString(eval.relativeAbsoluteError(), 12, 4).trim();
                RRSE = Utils.doubleToString(eval.rootRelativeSquaredError(), 12, 4).trim();
            } catch (Exception exception) {
                exception.printStackTrace();
            }

        }

        @Override
        public String toString() {
            return (CC + "\t"
                    + MAE + "\t"
                    + RMSE + "\t"
                    + RAE + "\t"
                    + RRSE);
        }

        public String toStringDetails() {
            return ("[score_training] CC: " + CC + "\n"
                    + "[score_training] MAE: " + MAE + "\n"
                    + "[score_training] RMSE: " + RMSE + "\n"
                    + "[score_training] RAE: " + RAE + "\n"
                    + "[score_training] RRSE: " + RRSE);
        }

        private void parsePredictions(StringBuffer sb) {
            String lines[] = sb.toString().split("\n");

            for (int i = 1; i < lines.length; i++) {
                String p = lines[i].replaceAll(" +", "\t");
                String tab[] = p.split("\t");
                String inst = tab[1];
                String actual = tab[2];
                String predicted = tab[3];
                String error = tab[4];
                String ID = tab[5];

                predictions.append(ID.replace("(", "").replace(")", "") + "\t" + actual + "\t" + predicted + "\t" + error + "\n");
            }

        }

        private void getFeatures(Instances data) {
            for (int i = 1; i < data.numAttributes() - 1; i++) {
                numberOfFeatures++;
                features.append(data.attribute(i).name() + "\n");
            }
        }

        private void getFeatures(Classifier model) {
            boolean voteClassifier = model.toString().split("\n")[0].contains("Vote");
            TreeMap<String, Integer> tm = new TreeMap<>();
            for (String s : model.toString().split("\n")) {
                if (s.startsWith("@attribute")) {
                    //hm.put(s.split(" ")[1], s.split(" ")[2]); //feature_name, feature_type
                    String name = s.split(" ")[1];
                    if (tm.containsKey(name)) {
                        int i = tm.get(name);
                        i++;
                        tm.put(name, i);
                    } else {
                        tm.put(s.split(" ")[1], 1);
                    }
                }

            }
            for (String f : tm.keySet()) {
                if (!f.equals("class")) {
                    numberOfFeatures++;
                    if (voteClassifier) {
                        features.append(f + "\t" + tm.get(f) + "\n");
                    } else {
                        features.append(f + "\n");
                    }
                }
            }
        }

        public String getFeatureRankingResults() {
            String[] s = featuresRankingResults.split("\n");
            StringBuilder toreturn = new StringBuilder();
            int index = 0;
            while (!s[index].equals("Ranked attributes:")) {
                index++;
            }
            index++;
            while (!s[index].startsWith("Selected")) {
                if (!s[index].contains(Main.mergingID) && !s[index].isEmpty()) {
                    String out = s[index].substring(1); //remove first space
                    out = out.replaceAll(" +", "\t");
                    out = out.split("\t")[0] + "\t" + out.split("\t")[2];
                    toreturn.append(out + "\n");
                }
                index++;
            }
            return toreturn.toString();
        }

    }

    public static class ClassificationResultsObject {

        public String ACC;
        public String MCC;
        public String TNR;//specificity
        public String AUC;
        public String TPR;//sensitivity
        public String Fscore;
        public double TP;
        public double FN;
        public double TN;
        public double FP;
        public String kappa;
        public StringBuilder predictions = new StringBuilder();
        public StringBuilder features = new StringBuilder();
        public int numberOfFeatures = 0; //number of features
        public Classifier model;
        public String featuresRankingResults;
        public Evaluation evaluation;
        public Instances dataset;
        public String FPR;
        public String recall;
        public String FNR;
        public String AUPRC;
        public String precision;
        public String FDR;
        public String matrix;
        public String pAUC;
        public String BCR;
        public String BER;

        private ClassificationResultsObject() {
        }

        public void getPredictions() {
            System.out.println("instance\tactual\tpredicted\terror\tprobability");
            System.out.println(predictions);
        }

        private ClassificationResultsObject(Evaluation eval, StringBuffer sb, Instances data, Classifier classifier) {
            dataset = data;
            evaluation = eval;
            try {
                parsePredictions(sb);
            } catch (Exception e) {
            }
            getFeatures(classifier);

            model = classifier;

            //measures
            ACC = Utils.doubleToString(eval.pctCorrect() / 100, 12, 4).trim();
            MCC = Utils.doubleToString(eval.weightedMatthewsCorrelation(), 12, 4).trim();
            TPR = Utils.doubleToString(eval.weightedTruePositiveRate(), 12, 4).trim();//sensitivity
            TNR = Utils.doubleToString(eval.weightedTrueNegativeRate(), 12, 4).trim();//specificity
            FPR = Utils.doubleToString(eval.weightedFalsePositiveRate(), 12, 4).trim();
            FNR = Utils.doubleToString(eval.weightedFalseNegativeRate(), 12, 4).trim();
            AUC = Utils.doubleToString(eval.weightedAreaUnderROC(), 12, 4).trim();
            AUPRC = Utils.doubleToString(eval.weightedAreaUnderPRC(), 12, 4).trim();
            Fscore = Utils.doubleToString(eval.weightedFMeasure(), 12, 4).trim();
            kappa = Utils.doubleToString(eval.kappa(), 12, 4).trim();
            precision = Utils.doubleToString(eval.weightedPrecision(), 12, 4).trim();
            FDR = Utils.doubleToString(1 - eval.weightedPrecision(), 12, 4).trim();
            recall = Utils.doubleToString(eval.weightedRecall(), 12, 4).trim();
            double[][] m = eval.confusionMatrix();
            matrix = "";
            for (int i = 0; i < m.length; i++) {
                matrix += "[";
                for (int j = 0; j < m.length; j++) {
                    matrix += (int) m[i][j] + " ";
                }
                matrix = matrix.substring(0, matrix.length() - 1) + "] ";
            }

            try {
                //pAUC
                int numberOfClasses = m.length;
                double pAUCs = 0;
                for (int i = 0; i < numberOfClasses; i++) {
                    Instances rocCurve = new ThresholdCurve().getCurve(eval.predictions(), i);
                    pAUCs += getPartialROCArea(rocCurve, Main.pAUC_lower, Main.pAUC_upper);
                }
                pAUC = Utils.doubleToString((pAUCs / (double) numberOfClasses), 12, 4).trim();
            } catch (Exception e) {
                pAUC = "NaN";
            }

            //balanced classification rate
            BCR = Utils.doubleToString(
                    ((eval.weightedTruePositiveRate() + eval.weightedTrueNegativeRate()) / m.length), 12, 4).trim();
            //BER (Balanced error rate)
            //https://arxiv.org/pdf/1207.3809.pdf
            BER = Utils.doubleToString(0.5 * ((eval.weightedFalsePositiveRate()) + (eval.weightedFalseNegativeRate())), 12, 4).trim();
//            BER = Utils.doubleToString(
//                    (1 - (eval.weightedTruePositiveRate() + eval.weightedTrueNegativeRate()) / m.length), 12, 4).trim();
        }

        @Override
        public String toString() {
            return (ACC + "\t"
                    + AUC + "\t"
                    + AUPRC + "\t"
                    + TPR + "\t" //SEN == recall
                    + TNR + "\t" //SPE
                    + MCC + "\t"
                    + BER + "\t"
                    + FPR + "\t"
                    + FNR + "\t"
                    + precision + "\t" //= PPV (positive predictive value)
                    + FDR + "\t"
                    + Fscore + "\t"
                    + kappa + "\t"
                    + matrix);
        }

        public String toStringShort() {
            return (ACC + "\t"
                    + AUC + "\t"
                    + AUPRC + "\t"
                    + TPR + "\t" //SEN == recall
                    + TNR + "\t" //SPE
                    + MCC + "\t"
                    + BER);
        }

        public String toStringDetails() {
            return ("[score_training] ACC: " + ACC + "\n"
                    + "[score_training] AUC: " + AUC + "\n"
                    + "[score_training] AUPRC: " + AUPRC + "\n"
                    + "[score_training] TPR: " + TPR + "\n"
                    + "[score_training] TNR: " + TNR + "\n"
                    + "[score_training] MCC: " + MCC + "\n"
                    + "[score_training] BER: " + BER + "\n"
                    + "[score_training] FPR: " + FPR + "\n"
                    + "[score_training] FNR: " + FNR + "\n"
                    + "[score_training] PPV: " + precision + "\n"
                    + "[score_training] FDR: " + FDR + "\n"
                    + "[score_training] recall: " + recall + "\n"
                    + "[score_training] Fscore: " + Fscore + "\n"
                    + "[score_training] kappa: " + kappa + "\n"
                    + "[score_training] matrix: " + matrix // + "pAUC[" + Main.pAUC_lower + "-" + Main.pAUC_upper + pAUC + "]: \n"
                    );
        }

        private void parsePredictions(StringBuffer sb) {
            String lines[] = sb.toString().split("\n");

            for (int i = 1; i < lines.length; i++) {
                String p = lines[i].replaceAll(" +", "\t");
                String tab[] = p.split("\t");
                String inst = tab[1];
                String actual = tab[2].split(":")[1];
                String predicted = tab[3].split(":")[1];
                String error;
                String prob;
                String ID;
                if (tab.length == 6) {
                    error = "0";
                    prob = tab[4];
                    ID = tab[5];
                } else {
                    error = "1";
                    prob = tab[5];
                    ID = tab[6];
                }
                predictions.append(ID.replace("(", "").replace(")", "") + "\t" + actual + "\t" + predicted + "\t" + error + "\t" + prob + "\n");
            }

        }

        private void getFeatures(Classifier model) {
            boolean voteClassifier = model.toString().split("\n")[0].contains("Vote");
            TreeMap<String, Integer> tm = new TreeMap<>();
            for (String s : model.toString().split("\n")) {
                if (s.startsWith("@attribute")) {
                    //hm.put(s.split(" ")[1], s.split(" ")[2]); //feature_name, feature_type
                    String name = s.split(" ")[1];
                    if (tm.containsKey(name)) {
                        int i = tm.get(name);
                        i++;
                        tm.put(name, i);
                    } else {
                        tm.put(s.split(" ")[1], 1);
                    }
                }

            }
            for (String f : tm.keySet()) {
                if (!f.equals("class")) {
                    numberOfFeatures++;
                    if (voteClassifier) {
                        features.append(f + "\t" + tm.get(f) + "\n");
                    } else {
                        features.append(f + "\n");
                    }
                }
            }
        }

        public String getFeatureRankingResults() {
            String[] s = featuresRankingResults.split("\n");
            StringBuilder toreturn = new StringBuilder();
            int index = 0;
            while (!s[index].equals("Ranked attributes:")) {
                index++;
            }
            index++;
            while (!s[index].startsWith("Selected")) {
                if (!s[index].contains(Main.mergingID) && !s[index].isEmpty()) {
                    String out = s[index].substring(1); //remove first space
                    out = out.replaceAll(" +", "\t");
                    String output = out.split("\t")[0] + "\t";
                    for (int i = 2; i < out.split("\t").length; i++) {
                        output += out.split("\t")[i] + " ";
                    }
                    toreturn.append(output + "\n");
                }
                index++;
            }
            return toreturn.toString();
        }
    }

    /**
     * compute partial AUC using trapezoid function
     */
    public static double getPartialROCArea(Instances tcurve, double limit_inf, double limit_sup) {
        int tpr = tcurve.attribute("True Positive Rate").index();
        int fpr = tcurve.attribute("False Positive Rate").index();
        double[] tprVals = tcurve.attributeToDoubleArray(tpr);
        double[] fprVals = tcurve.attributeToDoubleArray(fpr);

        //retrive coordinates
        //transform list type in arraylist to revert them after
        ArrayList<Double> alx = new ArrayList<>();
        ArrayList<Double> aly = new ArrayList<>();

        for (int i = 0; i < fprVals.length; i++) {
            alx.add(fprVals[i]);
            aly.add(tprVals[i]);
        }

        Collections.reverse(alx);
        Collections.reverse(aly);

        //create function points
        double[] x = new double[alx.size()];
        double[] y = new double[aly.size()];
        for (int i = 0; i < alx.size(); i++) {
            x[i] = alx.get(i);
            y[i] = aly.get(i);
        }
        //correct to avoid multiple x values
        try {
            for (int i = 1; i < x.length; i++) {
                if (x[i] <= x[i - 1]) {
                    x[i] = x[i - 1] + 0.0001;
                    int j = i + 1;
                    while (x[j] == x[i - 1] && j < x.length - 1) {
                        x[j] += 0.0001;
                        j++;
                    }
                }
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        //show curve
//        System.out.println("x\ty");
//        for (int i = 0; i < y.length; i++) {
//            System.out.println(x[i]+"\t"+y[i]);
//
//        }

        //https://stackoverflow.com/questions/16896961/how-to-compute-integration-of-a-function-in-apache-commons-math3-library
        //estimate function
        UnivariateInterpolator interpolator = new SplineInterpolator();
        UnivariateFunction function = interpolator.interpolate(x, y);
        //compute integrale
        TrapezoidIntegrator trapezoid = new TrapezoidIntegrator();
        double pauc = 0;
        try {
            pauc = trapezoid.integrate(10000000, function, limit_inf, 1);
            //because of the spline curve, some area can be counter as 0
            if (pauc < 0) {
                return 0;
            }
        } catch (MaxCountExceededException maxCountExceededException) {
            //pauc = trapezoid.integrate(10000000, function, limit_inf, limit_sup);
        }

        return pauc;
    }

    public static class bootstrapResultsObject {

        public ArrayList<Double> alAUCs = new ArrayList<>();
        public ArrayList<Double> alpAUCs = new ArrayList<>();
        public ArrayList<Double> alAUPRCs = new ArrayList<>();
        public ArrayList<Double> alACCs = new ArrayList<>();
        public ArrayList<Double> alSEs = new ArrayList<>();
        public ArrayList<Double> alSPs = new ArrayList<>();
        public ArrayList<Double> alMCCs = new ArrayList<>();
        public ArrayList<Double> alBERs = new ArrayList<>();
        public String meanACCs;
        public String meanAUCs;
        public String meanAUPRCs;
        public String meanSEs;
        public String meanSPs;
        public String meanMCCs;
        public String meanBERs;
        public ArrayList<Double> alCCs = new ArrayList<>();
        public ArrayList<Double> alRMSEs = new ArrayList<>();
        public ArrayList<Double> alMAEs = new ArrayList<>();
        public ArrayList<Double> alRAEs = new ArrayList<>();
        public ArrayList<Double> alRRSEs = new ArrayList<>();
        public String meanCCs;
        public String meanMAEs;
        public String meanRMSEs;
        public String meanRAEs;
        public String meanRRSEs;

        public bootstrapResultsObject() {
        }

        public String toStringClassification() {
            if (meanACCs == null) {
                return "\t\t\t\t\t\t";
            } else {
                return meanACCs + "\t"
                        + meanAUCs + "\t"
                        + meanAUPRCs + "\t"
                        + meanSEs + "\t"
                        + meanSPs + "\t"
                        + meanMCCs + "\t"
                        + meanBERs;
            }
        }

        public String toStringClassificationDetails() {
            return "[score_training] Average weighted ACC: " + meanACCs + "\n"
                    + "[score_training] Average weighted AUC: " + meanAUCs + "\n"
                    + "[score_training] Average weighted AUPRC: " + meanAUPRCs + "\n"
                    + "[score_training] Average weighted SEN: " + meanSEs + "\n"
                    + "[score_training] Average weighted SPE: " + meanSPs + "\n"
                    + "[score_training] Average weighted MCC: " + meanMCCs + "\n"
                    + "[score_training] Average weighted BER: " + meanBERs;
        }

        public String toStringRegression() {
            if (meanCCs == null) {
                return "\t\t\t\t";
            } else {
                return meanCCs + "\t"
                        + meanMAEs + "\t"
                        + meanRMSEs + "\t"
                        + meanRAEs + "\t"
                        + meanRRSEs;
            }
        }

        public String toStringRegressionDetails() {
            return "[score_training] Average weighted CC: " + meanCCs + "\n"
                    + "[score_training] Average weighted MAE: " + meanMAEs + "\n"
                    + "[score_training] Average weighted RMSE: " + meanRMSEs + "\n"
                    + "[score_training] Average weighted RAE: " + meanRAEs + "\n"
                    + "[score_training] Average weighted RRSE: " + meanRRSEs;
        }

        public void computeMeans() {
            meanACCs = getMean(alACCs);
            meanAUCs = getMean(alAUCs);
            meanAUPRCs = getMean(alAUPRCs);
            meanSEs = getMean(alSEs);
            meanSPs = getMean(alSPs);
            meanMCCs = getMean(alMCCs);
            meanBERs = getMean(alBERs);
            meanCCs = getMean(alCCs);
            meanRMSEs = getMean(alRMSEs);
        }
    }

    public static class testResultsObject {

        public String ACC;
        public String AUC;
        public String AUPRC;
        public String SP; //TNR
        public String SE; //TPR
        public String MCC;
        public String BER;
        public String CC;
        public String MAE;
        public String RMSE;
        public String RAE;
        public String RRSE;

        public testResultsObject() {
        }

        public String toStringClassification() {
            return ACC + "\t"
                    + AUC + "\t"
                    + AUPRC + "\t"
                    + SE + "\t"
                    + SP + "\t"
                    + MCC + "\t"
                    + BER;
        }

        public String toStringRegression() {
            return CC + "\t"
                    + MAE + "\t"
                    + RMSE + "\t"
                    + RAE + "\t"
                    + RRSE;
        }

    }
}