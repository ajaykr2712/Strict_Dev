import java.util.*;

// Command Pattern + ML Operations
public class MLCommandPattern {
    interface MLCommand {
        void execute();
        void undo();
        String getDescription();
    }
    
    // ML Context - holds the model and data
    static class MLContext {
        private double[][] trainData;
        private double[] trainLabels;
        private double[][] testData;
        private double[] testLabels;
        private Map<String, Object> model;
        private Map<String, Double> metrics;
        private List<String> history;
        
        MLContext() {
            this.model = new HashMap<>();
            this.metrics = new HashMap<>();
            this.history = new ArrayList<>();
        }
        
        // Getters and setters
        void setTrainData(double[][] data, double[] labels) {
            this.trainData = data;
            this.trainLabels = labels;
        }
        
        void setTestData(double[][] data, double[] labels) {
            this.testData = data;
            this.testLabels = labels;
        }
        
        double[][] getTrainData() { return trainData; }
        double[] getTrainLabels() { return trainLabels; }
        double[][] getTestData() { return testData; }
        double[] getTestLabels() { return testLabels; }
        Map<String, Object> getModel() { return model; }
        Map<String, Double> getMetrics() { return metrics; }
        List<String> getHistory() { return history; }
        
        void addToHistory(String action) {
            history.add(action);
        }
    }
    
    static class TrainModelCommand implements MLCommand {
        private final MLContext context;
        private final String algorithm;
        private Map<String, Object> previousModel;
        
        TrainModelCommand(MLContext context, String algorithm) {
            this.context = context;
            this.algorithm = algorithm;
        }
        
        @Override
        public void execute() {
            // Save previous model state for undo
            previousModel = new HashMap<>(context.getModel());
            
            // Simple linear regression training simulation
            double[][] X = context.getTrainData();
            double[] y = context.getTrainLabels();
            
            if (X == null || y == null) {
                throw new IllegalStateException("Training data not set");
            }
            
            switch (algorithm.toLowerCase()) {
                case "linear_regression":
                    trainLinearRegression(X, y);
                    break;
                case "logistic_regression":
                    trainLogisticRegression(X, y);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
            }
            
            context.addToHistory("Trained " + algorithm + " model");
            System.out.println("Model trained: " + algorithm);
        }
        
        private void trainLinearRegression(double[][] X, double[] y) {
            // Simple implementation for demonstration
            double[] weights = new double[X[0].length];
            double bias = 0;
            
            // Gradient descent (simplified)
            double learningRate = 0.01;
            for (int epoch = 0; epoch < 100; epoch++) {
                double[] gradients = new double[weights.length];
                double biasGradient = 0;
                
                for (int i = 0; i < X.length; i++) {
                    double prediction = bias;
                    for (int j = 0; j < weights.length; j++) {
                        prediction += weights[j] * X[i][j];
                    }
                    double error = prediction - y[i];
                    
                    for (int j = 0; j < weights.length; j++) {
                        gradients[j] += error * X[i][j];
                    }
                    biasGradient += error;
                }
                
                for (int j = 0; j < weights.length; j++) {
                    weights[j] -= learningRate * gradients[j] / X.length;
                }
                bias -= learningRate * biasGradient / X.length;
            }
            
            context.getModel().put("algorithm", "linear_regression");
            context.getModel().put("weights", weights);
            context.getModel().put("bias", bias);
        }
        
        private void trainLogisticRegression(double[][] X, double[] y) {
            // Simplified logistic regression
            double[] weights = new double[X[0].length];
            double bias = 0;
            
            double learningRate = 0.1;
            for (int epoch = 0; epoch < 100; epoch++) {
                double[] gradients = new double[weights.length];
                double biasGradient = 0;
                
                for (int i = 0; i < X.length; i++) {
                    double z = bias;
                    for (int j = 0; j < weights.length; j++) {
                        z += weights[j] * X[i][j];
                    }
                    double prediction = 1.0 / (1.0 + Math.exp(-z));
                    double error = prediction - y[i];
                    
                    for (int j = 0; j < weights.length; j++) {
                        gradients[j] += error * X[i][j];
                    }
                    biasGradient += error;
                }
                
                for (int j = 0; j < weights.length; j++) {
                    weights[j] -= learningRate * gradients[j] / X.length;
                }
                bias -= learningRate * biasGradient / X.length;
            }
            
            context.getModel().put("algorithm", "logistic_regression");
            context.getModel().put("weights", weights);
            context.getModel().put("bias", bias);
        }
        
        @Override
        public void undo() {
            if (previousModel != null) {
                context.getModel().clear();
                context.getModel().putAll(previousModel);
                context.addToHistory("Undid training of " + algorithm);
                System.out.println("Undid model training");
            }
        }
        
        @Override
        public String getDescription() {
            return "Train " + algorithm + " model";
        }
    }
    
    static class EvaluateModelCommand implements MLCommand {
        private final MLContext context;
        private Map<String, Double> previousMetrics;
        
        EvaluateModelCommand(MLContext context) {
            this.context = context;
        }
        
        @Override
        public void execute() {
            // Save previous metrics for undo
            previousMetrics = new HashMap<>(context.getMetrics());
            
            if (context.getModel().isEmpty()) {
                throw new IllegalStateException("No model to evaluate");
            }
            
            double[][] X = context.getTestData();
            double[] y = context.getTestLabels();
            
            if (X == null || y == null) {
                throw new IllegalStateException("Test data not set");
            }
            
            String algorithm = (String) context.getModel().get("algorithm");
            double[] weights = (double[]) context.getModel().get("weights");
            double bias = (Double) context.getModel().get("bias");
            
            double totalError = 0;
            int correct = 0;
            
            for (int i = 0; i < X.length; i++) {
                double prediction = bias;
                for (int j = 0; j < weights.length; j++) {
                    prediction += weights[j] * X[i][j];
                }
                
                if ("logistic_regression".equals(algorithm)) {
                    prediction = 1.0 / (1.0 + Math.exp(-prediction));
                    prediction = prediction >= 0.5 ? 1.0 : 0.0;
                    if (Math.abs(prediction - y[i]) < 0.1) correct++;
                } else {
                    double error = prediction - y[i];
                    totalError += error * error;
                }
            }
            
            if ("logistic_regression".equals(algorithm)) {
                double accuracy = (double) correct / X.length;
                context.getMetrics().put("accuracy", accuracy);
                System.out.printf("Model accuracy: %.3f%n", accuracy);
            } else {
                double mse = totalError / X.length;
                double rmse = Math.sqrt(mse);
                context.getMetrics().put("mse", mse);
                context.getMetrics().put("rmse", rmse);
                System.out.printf("Model RMSE: %.3f%n", rmse);
            }
            
            context.addToHistory("Evaluated model");
        }
        
        @Override
        public void undo() {
            if (previousMetrics != null) {
                context.getMetrics().clear();
                context.getMetrics().putAll(previousMetrics);
                context.addToHistory("Undid model evaluation");
                System.out.println("Undid model evaluation");
            }
        }
        
        @Override
        public String getDescription() {
            return "Evaluate model performance";
        }
    }
    
    static class PredictCommand implements MLCommand {
        private final MLContext context;
        private final double[] input;
        private double result;
        
        PredictCommand(MLContext context, double[] input) {
            this.context = context;
            this.input = input.clone();
        }
        
        @Override
        public void execute() {
            if (context.getModel().isEmpty()) {
                throw new IllegalStateException("No model available for prediction");
            }
            
            String algorithm = (String) context.getModel().get("algorithm");
            double[] weights = (double[]) context.getModel().get("weights");
            double bias = (Double) context.getModel().get("bias");
            
            result = bias;
            for (int i = 0; i < weights.length; i++) {
                result += weights[i] * input[i];
            }
            
            if ("logistic_regression".equals(algorithm)) {
                result = 1.0 / (1.0 + Math.exp(-result));
            }
            
            System.out.printf("Prediction for %s: %.3f%n", Arrays.toString(input), result);
            context.addToHistory("Made prediction");
        }
        
        @Override
        public void undo() {
            // Predictions don't typically need undo, but we can log it
            context.addToHistory("Undid prediction command");
            System.out.println("Undid prediction command");
        }
        
        @Override
        public String getDescription() {
            return "Predict using model";
        }
        
        double getResult() {
            return result;
        }
    }
    
    static class MLInvoker {
        private final List<MLCommand> commandHistory = new ArrayList<>();
        
        void executeCommand(MLCommand command) {
            command.execute();
            commandHistory.add(command);
        }
        
        void undoLastCommand() {
            if (!commandHistory.isEmpty()) {
                MLCommand lastCommand = commandHistory.remove(commandHistory.size() - 1);
                lastCommand.undo();
            } else {
                System.out.println("No commands to undo");
            }
        }
        
        void showCommandHistory() {
            System.out.println("Command History:");
            for (int i = 0; i < commandHistory.size(); i++) {
                System.out.println((i + 1) + ". " + commandHistory.get(i).getDescription());
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("ML Command Pattern Demo\n");
        
        // Create ML context
        MLContext context = new MLContext();
        MLInvoker invoker = new MLInvoker();
        
        // Set up sample data
        double[][] trainX = {{1, 2}, {2, 3}, {3, 4}, {4, 5}};
        double[] trainY = {3, 5, 7, 9}; // y = x1 + x2
        double[][] testX = {{5, 6}, {6, 7}};
        double[] testY = {11, 13};
        
        context.setTrainData(trainX, trainY);
        context.setTestData(testX, testY);
        
        // Execute commands
        System.out.println("=== Training Linear Regression ===");
        MLCommand trainCommand = new TrainModelCommand(context, "linear_regression");
        invoker.executeCommand(trainCommand);
        
        System.out.println("\n=== Evaluating Model ===");
        MLCommand evaluateCommand = new EvaluateModelCommand(context);
        invoker.executeCommand(evaluateCommand);
        
        System.out.println("\n=== Making Predictions ===");
        MLCommand predictCommand1 = new PredictCommand(context, new double[]{7, 8});
        invoker.executeCommand(predictCommand1);
        
        MLCommand predictCommand2 = new PredictCommand(context, new double[]{10, 12});
        invoker.executeCommand(predictCommand2);
        
        System.out.println("\n=== Command History ===");
        invoker.showCommandHistory();
        
        System.out.println("\n=== Undoing Last Command ===");
        invoker.undoLastCommand();
        
        System.out.println("\n=== ML Context History ===");
        for (String action : context.getHistory()) {
            System.out.println("- " + action);
        }
        
        System.out.println("\n=== Training Logistic Regression ===");
        // Change to classification data
        double[] classificationY = {0, 0, 1, 1};
        context.setTrainData(trainX, classificationY);
        context.setTestData(testX, new double[]{1, 1});
        
        MLCommand logisticTrainCommand = new TrainModelCommand(context, "logistic_regression");
        invoker.executeCommand(logisticTrainCommand);
        
        MLCommand logisticEvaluateCommand = new EvaluateModelCommand(context);
        invoker.executeCommand(logisticEvaluateCommand);
        
        System.out.println("\nFinal command history:");
        invoker.showCommandHistory();
    }
}
