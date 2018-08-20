package au.edu.unimelb.processmining.accuracy;

import au.edu.qut.processmining.log.LogParser;
import au.edu.qut.processmining.log.SimpleLog;
import au.edu.unimelb.processmining.accuracy.abstraction.Abstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.intermediate.AutomatonAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.LogAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.markovian.MarkovAbstraction;
import au.edu.unimelb.processmining.accuracy.abstraction.subtrace.SubtraceAbstraction;
import com.raffaeleconforti.context.FakePluginContext;
import com.raffaeleconforti.log.util.LogImporter;
import de.drscc.automaton.Automaton;
import de.drscc.importer.ImportProcessModel;

import au.edu.unimelb.processmining.accuracy.abstraction.ProcessAbstraction;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.transitionsystem.TransitionSystem;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.tsml.importing.TsmlImportTS;

/**
 * Created by Adriano on 23/01/18.
 */
public class MarkovianAccuracyCalculator {
    public enum Abs {MARK, STA}
    public enum Opd {SPL, HUN, GRD}

    private SimpleLog log;
    private Automaton automaton;
    private AutomatonAbstraction automatonAbstraction;
    private Abstraction logAbstraction, processAbstraction;
    private int order;


    public double[] accuracy(Abs type, Opd opd, String logP, String processP, int order) {
        double[] accuracy = new double[3];

        try {
            this.order = order;
            if( importLogFromFile(logP, type) && importProcessFromFile(processP, type) ) {
                accuracy[0] = computeFitness(opd);
                accuracy[1] = computePrecision(opd);
                accuracy[2] = (2.0 * accuracy[0] * accuracy[1])/(accuracy[0] + accuracy[1]);
                System.out.println("RESULT - fscore: " + accuracy[2]);
            } else System.out.println("ERROR - something went wrong importing the inputs.");
        } catch(StackOverflowError sofe) {
            System.out.println("RESULT(sofe) - fscore: " + accuracy);
//            sofe.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - something went wrong with the GED.");
        }
        return accuracy;
    }

    public double precision(Abs type, Opd opd, String logP, String processP, int order) {
        double precision = -1.0;

        try {
            this.order = order;
            if( importLogFromFile(logP, type) && importProcessFromFile(processP, type) ) precision = computePrecision(opd);
            else System.out.println("ERROR - something went wrong importing the inputs.");
        } catch(StackOverflowError sofe) {
            precision = 0.0;
            System.out.println("RESULT(sofe) - precision: " + precision);
//            sofe.printStackTrace();
            return 0.0;
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - something went wrong with the GED.");
        }

        return precision;
    }

    public double fitness(Abs type, Opd opd, String logP, String processP, int order) {
        double fitness = -1.0;

        try {
            this.order = order;
            if( importLogFromFile(logP, type) && importProcessFromFile(processP, type) ) fitness = computeFitness(opd);
            else System.out.println("ERROR - something went wrong importing the inputs.");
        } catch(StackOverflowError sofe) {
            fitness = 0.0;
            System.out.println("RESULT(sofe) - fitness: " + fitness);
//            sofe.printStackTrace();
            return 0.0;
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - something went wrong with the GED.");
        }

        return fitness;
    }

    private double computePrecision(Opd opd) {
        double precision = 0.0;

        switch(opd) {
            case SPL:
                precision = processAbstraction.minus(logAbstraction);
                break;
            case HUN:
                precision = processAbstraction.minusHUN(logAbstraction);
                break;
            case GRD:
                precision = processAbstraction.minusGRD(logAbstraction);
                break;
        }

        System.out.println("RESULT - precision: " + precision);
        return precision;
    }


    private double computeFitness(Opd opd) {
        double fitness = 0.0;

        switch(opd) {
            case SPL:
                fitness = logAbstraction.minus(processAbstraction);
                break;
            case HUN:
                fitness = logAbstraction.minusHUN(processAbstraction);
                break;
            case GRD:
                fitness = logAbstraction.minusGRD(processAbstraction);
                break;
        }

        System.out.println("RESULT - fitness: " + fitness);
        return fitness;
    }

    private boolean importLogFromFile(String lopP, Abs type) {
        XLog xlog;
        System.out.println("INFO - input log: " + lopP);
        try{
            if(!lopP.endsWith(".txt")) {
                xlog = LogImporter.importFromFile(new XFactoryNaiveImpl(), lopP);
                log = LogParser.getSimpleLog(xlog, new XEventNameClassifier());
            } else log = LogParser.getSimpleLog(lopP);

//            for(String s : log.getReverseMap().keySet()) System.out.println("DEBUG - log activity: " + s);

            switch(type) {
                case MARK:
                    logAbstraction = LogAbstraction.markovian(log, order);
                    break;
                case STA:
                    logAbstraction = LogAbstraction.subtrace(log, order);
                    logAbstraction.print();
                    break;
            }

//            logAbstraction.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the log file.");
            return false;
        }
    }

    private boolean importLog (XLog xlog, Abs type){
        try{
            log = LogParser.getSimpleLog(xlog, new XEventNameClassifier());

            switch(type) {
                case MARK:
                    logAbstraction = LogAbstraction.markovian(log, order);
                    break;
                case STA:
                    logAbstraction = LogAbstraction.subtrace(log, order);
                    break;
            }
//            logAbstraction.print();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the log file.");
            return false;
        }
    }

    private boolean importProcessFromFile(String processP, Abs type) {
        ImportProcessModel importer = new ImportProcessModel();
        TransitionSystem transitionSystem = null;
        TsmlImportTS tsImporter;
        Object results;
        System.out.println("INFO - input process: " + processP);

        try {
            if( processP.contains(".bpmn") ) automaton = importer.createFSMfromBPNMFileWithConversion(processP, null, null);
            else if(processP.contains(".pnml")) automaton = importer.createFSMfromPNMLFile(processP, null, null);
            else if(processP.contains(".tsml")) {
                tsImporter = new TsmlImportTS();
                results = tsImporter.importFile(new FakePluginContext(), processP);
                if( results instanceof Object[] )
                    if( ((Object[])results)[0] instanceof TransitionSystem ) {
//                        System.out.println("DEBUG - EUREKA");
                        transitionSystem = (TransitionSystem) ((Object[])results)[0];
                    }
            }

            if( transitionSystem == null ) {
//                System.out.println("INFO - D-Automaton (" + automaton.states().size() + "," + automaton.transitions().size() + ")");
                automatonAbstraction = new AutomatonAbstraction(automaton, log);
            } else {
                automatonAbstraction = new AutomatonAbstraction(transitionSystem, log);
//                System.out.println("INFO - TS-Automaton (" + automatonAbstraction.getNodes().size() + "," + automatonAbstraction.getEdges().size() + ")");
            }

            switch(type) {
                case MARK:
                    automatonAbstraction.generateMarkovianLabels(order);
                    processAbstraction = ProcessAbstraction.markovian(automatonAbstraction);
                    break;
                case STA:
                    processAbstraction = new SubtraceAbstraction();
                    break;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the process file.");
            return false;
        }
    }


    private boolean importPetrinet(Petrinet petrinet,  Marking initialMarking, Abs type) {
        ImportProcessModel importer = new ImportProcessModel();

        try {
            automaton = importer.createFSMfromPetrinet(petrinet, initialMarking, null, null);
            automatonAbstraction = new AutomatonAbstraction(automaton, log);

            switch(type) {
                case MARK:
                    automatonAbstraction.generateMarkovianLabels(order);
                    processAbstraction = ProcessAbstraction.markovian(automatonAbstraction);
                    break;
                case STA:
                    processAbstraction = new SubtraceAbstraction();
                    break;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR - impossible to read the process file.");
            return false;
        }
    }



}
