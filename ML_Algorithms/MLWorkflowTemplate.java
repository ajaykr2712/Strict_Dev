import java.util.*;

// Template Method Pattern + ML Workflow
public class MLWorkflowTemplate {
    abstract static class MLWorkflow {
        // Template method - defines the algorithm structure
        public final void executeWorkflow(double[][] X, double[] y) {
            System.out.println("=== Starting ML Workflow ===");
            
            preprocessData(X, y);
            splitData(X, y);
            trainModel();
            validateModel();
            evaluateModel();
            deployModel();
            
            System.out.println("=== Workflow Complete ===\n");
        }
        
        // Abstract methods to be implemented by subclasses
        protected abstract void preprocessData(double[][] X, double[] y);
        protected abstract void trainModel();
        protected abstract void validateModel();
        
        // Concrete methods with default implementations
        protected void splitData(double[][] X, double[] y) {
            System.out.println("Splitting data into train/validation sets (80/20)");
        }
        
        protected void evaluateModel() {
            System.out.println("Evaluating model performance on test set");
        }
        
        protected void deployModel() {
            System.out.println("Model ready for deployment");
        }
    }
    
    static class SupervisedLearningWorkflow extends MLWorkflow {
        private final String modelType;
        
        SupervisedLearningWorkflow(String modelType) {
            this.modelType = modelType;
        }
        
        @Override
        protected void preprocessData(double[][] X, double[] y) {
            System.out.println("Preprocessing: Normalizing features, handling missing values");
            System.out.println("Feature scaling applied to " + X[0].length + " features");
        }
        
        @Override
        protected void trainModel() {
            System.out.println("Training " + modelType + " model with supervised learning");
            System.out.println("Using gradient descent optimization");
        }
        
        @Override
        protected void validateModel() {
            System.out.println("Cross-validation with k=5 folds");
            System.out.println("Hyperparameter tuning completed");
        }
    }
    
    static class UnsupervisedLearningWorkflow extends MLWorkflow {
        private final String algorithm;
        
        UnsupervisedLearningWorkflow(String algorithm) {
            this.algorithm = algorithm;
        }
        
        @Override
        protected void preprocessData(double[][] X, double[] y) {
            System.out.println("Preprocessing: Feature scaling and dimensionality reduction");
            System.out.println("PCA applied for dimensionality reduction");
        }
        
        @Override
        protected void trainModel() {
            System.out.println("Training " + algorithm + " clustering algorithm");
            System.out.println("Determining optimal number of clusters");
        }
        
        @Override
        protected void validateModel() {
            System.out.println("Validating clusters using silhouette score");
            System.out.println("Checking cluster stability");
        }
        
        @Override
        protected void evaluateModel() {
            System.out.println("Evaluating clustering quality metrics");
            System.out.println("Visualizing cluster distributions");
        }
    }
    
    static class DeepLearningWorkflow extends MLWorkflow {
        private final String architecture;
        
        DeepLearningWorkflow(String architecture) {
            this.architecture = architecture;
        }
        
        @Override
        protected void preprocessData(double[][] X, double[] y) {
            System.out.println("Preprocessing: Data augmentation and normalization");
            System.out.println("Converting data to tensors");
        }
        
        @Override
        protected void trainModel() {
            System.out.println("Training " + architecture + " neural network");
            System.out.println("Using Adam optimizer with learning rate scheduling");
        }
        
        @Override
        protected void validateModel() {
            System.out.println("Monitoring training/validation loss curves");
            System.out.println("Early stopping to prevent overfitting");
        }
        
        @Override
        protected void splitData(double[][] X, double[] y) {
            System.out.println("Splitting data into train/validation/test sets (70/15/15)");
        }
        
        @Override
        protected void evaluateModel() {
            System.out.println("Evaluating on test set with comprehensive metrics");
            System.out.println("Generating confusion matrix and classification report");
        }
        
        @Override
        protected void deployModel() {
            System.out.println("Converting model to production format");
            System.out.println("Setting up model serving infrastructure");
        }
    }
    
    // Hook method example - allows workflows to add custom steps
    abstract static class CustomizableMLWorkflow extends MLWorkflow {
        @Override
        public final void executeWorkflow(double[][] X, double[] y) {
            System.out.println("=== Starting Customizable ML Workflow ===");
            
            preprocessData(X, y);
            
            if (shouldPerformFeatureSelection()) {
                performFeatureSelection(X, y);
            }
            
            splitData(X, y);
            trainModel();
            validateModel();
            
            if (shouldPerformEnsembling()) {
                performEnsembling();
            }
            
            evaluateModel();
            deployModel();
            
            System.out.println("=== Customizable Workflow Complete ===\n");
        }
        
        // Hook methods - subclasses can override to customize behavior
        protected boolean shouldPerformFeatureSelection() {
            return false;
        }
        
        protected boolean shouldPerformEnsembling() {
            return false;
        }
        
        protected void performFeatureSelection(double[][] X, double[] y) {
            System.out.println("Performing feature selection");
        }
        
        protected void performEnsembling() {
            System.out.println("Creating ensemble of models");
        }
    }
    
    static class AdvancedClassificationWorkflow extends CustomizableMLWorkflow {
        @Override
        protected void preprocessData(double[][] X, double[] y) {
            System.out.println("Advanced preprocessing: SMOTE for class imbalance");
        }
        
        @Override
        protected void trainModel() {
            System.out.println("Training ensemble of classification models");
        }
        
        @Override
        protected void validateModel() {
            System.out.println("Stratified cross-validation");
        }
        
        @Override
        protected boolean shouldPerformFeatureSelection() {
            return true;
        }
        
        @Override
        protected boolean shouldPerformEnsembling() {
            return true;
        }
        
        @Override
        protected void performFeatureSelection(double[][] X, double[] y) {
            System.out.println("Recursive feature elimination with cross-validation");
        }
        
        @Override
        protected void performEnsembling() {
            System.out.println("Stacking ensemble with meta-learner");
        }
    }
    
    public static void main(String[] args) {
        // Sample dataset
        double[][] X = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};
        double[] y = {0, 1, 0, 1};
        
        System.out.println("Template Method Pattern for ML Workflows\n");
        
        // Supervised Learning Workflow
        MLWorkflow supervisedWorkflow = new SupervisedLearningWorkflow("Random Forest");
        supervisedWorkflow.executeWorkflow(X, y);
        
        // Unsupervised Learning Workflow
        MLWorkflow unsupervisedWorkflow = new UnsupervisedLearningWorkflow("K-Means");
        unsupervisedWorkflow.executeWorkflow(X, y);
        
        // Deep Learning Workflow
        MLWorkflow deepLearningWorkflow = new DeepLearningWorkflow("CNN");
        deepLearningWorkflow.executeWorkflow(X, y);
        
        // Advanced Classification Workflow with hooks
        MLWorkflow advancedWorkflow = new AdvancedClassificationWorkflow();
        advancedWorkflow.executeWorkflow(X, y);
    }
}
