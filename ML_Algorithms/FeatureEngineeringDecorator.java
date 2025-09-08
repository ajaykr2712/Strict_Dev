import java.util.*;

// Decorator Pattern + Feature Engineering
public class FeatureEngineeringDecorator {
    interface DataProcessor {
        double[][] process(double[][] data);
        String getDescription();
    }
    
    static class BaseDataProcessor implements DataProcessor {
        @Override
        public double[][] process(double[][] data) {
            return data; // Return data as-is
        }
        
        @Override
        public String getDescription() {
            return "Raw data";
        }
    }
    
    abstract static class FeatureDecorator implements DataProcessor {
        protected DataProcessor processor;
        
        FeatureDecorator(DataProcessor processor) {
            this.processor = processor;
        }
        
        @Override
        public double[][] process(double[][] data) {
            return processor.process(data);
        }
        
        @Override
        public String getDescription() {
            return processor.getDescription();
        }
    }
    
    static class NormalizationDecorator extends FeatureDecorator {
        NormalizationDecorator(DataProcessor processor) {
            super(processor);
        }
        
        @Override
        public double[][] process(double[][] data) {
            double[][] processedData = super.process(data);
            return normalize(processedData);
        }
        
        private double[][] normalize(double[][] data) {
            int rows = data.length;
            int cols = data[0].length;
            double[][] normalized = new double[rows][cols];
            
            // Find min and max for each column
            double[] mins = new double[cols];
            double[] maxs = new double[cols];
            Arrays.fill(mins, Double.MAX_VALUE);
            Arrays.fill(maxs, Double.MIN_VALUE);
            
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    mins[j] = Math.min(mins[j], data[i][j]);
                    maxs[j] = Math.max(maxs[j], data[i][j]);
                }
            }
            
            // Normalize to [0, 1]
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (maxs[j] != mins[j]) {
                        normalized[i][j] = (data[i][j] - mins[j]) / (maxs[j] - mins[j]);
                    } else {
                        normalized[i][j] = 0;
                    }
                }
            }
            
            return normalized;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " -> Normalized";
        }
    }
    
    static class StandardizationDecorator extends FeatureDecorator {
        StandardizationDecorator(DataProcessor processor) {
            super(processor);
        }
        
        @Override
        public double[][] process(double[][] data) {
            double[][] processedData = super.process(data);
            return standardize(processedData);
        }
        
        private double[][] standardize(double[][] data) {
            int rows = data.length;
            int cols = data[0].length;
            double[][] standardized = new double[rows][cols];
            
            // Calculate mean and standard deviation for each column
            double[] means = new double[cols];
            double[] stds = new double[cols];
            
            // Calculate means
            for (int j = 0; j < cols; j++) {
                double sum = 0;
                for (int i = 0; i < rows; i++) {
                    sum += data[i][j];
                }
                means[j] = sum / rows;
            }
            
            // Calculate standard deviations
            for (int j = 0; j < cols; j++) {
                double sumSquares = 0;
                for (int i = 0; i < rows; i++) {
                    sumSquares += (data[i][j] - means[j]) * (data[i][j] - means[j]);
                }
                stds[j] = Math.sqrt(sumSquares / rows);
            }
            
            // Standardize
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (stds[j] != 0) {
                        standardized[i][j] = (data[i][j] - means[j]) / stds[j];
                    } else {
                        standardized[i][j] = 0;
                    }
                }
            }
            
            return standardized;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " -> Standardized";
        }
    }
    
    static class PolynomialFeaturesDecorator extends FeatureDecorator {
        private final int degree;
        
        PolynomialFeaturesDecorator(DataProcessor processor, int degree) {
            super(processor);
            this.degree = degree;
        }
        
        @Override
        public double[][] process(double[][] data) {
            double[][] processedData = super.process(data);
            return addPolynomialFeatures(processedData);
        }
        
        private double[][] addPolynomialFeatures(double[][] data) {
            int rows = data.length;
            int originalCols = data[0].length;
            
            // Calculate number of polynomial features
            int newCols = originalCols;
            if (degree >= 2) {
                // Add squared terms
                newCols += originalCols;
                // Add interaction terms for degree 2
                newCols += (originalCols * (originalCols - 1)) / 2;
            }
            
            double[][] enhanced = new double[rows][newCols];
            
            for (int i = 0; i < rows; i++) {
                int colIndex = 0;
                
                // Original features
                for (int j = 0; j < originalCols; j++) {
                    enhanced[i][colIndex++] = data[i][j];
                }
                
                if (degree >= 2) {
                    // Squared terms
                    for (int j = 0; j < originalCols; j++) {
                        enhanced[i][colIndex++] = data[i][j] * data[i][j];
                    }
                    
                    // Interaction terms
                    for (int j = 0; j < originalCols; j++) {
                        for (int k = j + 1; k < originalCols; k++) {
                            enhanced[i][colIndex++] = data[i][j] * data[i][k];
                        }
                    }
                }
            }
            
            return enhanced;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " -> Polynomial(degree=" + degree + ")";
        }
    }
    
    static class OutlierRemovalDecorator extends FeatureDecorator {
        private final double threshold;
        
        OutlierRemovalDecorator(DataProcessor processor, double threshold) {
            super(processor);
            this.threshold = threshold; // Z-score threshold
        }
        
        @Override
        public double[][] process(double[][] data) {
            double[][] processedData = super.process(data);
            return removeOutliers(processedData);
        }
        
        private double[][] removeOutliers(double[][] data) {
            List<double[]> cleanedData = new ArrayList<>();
            int cols = data[0].length;
            
            // Calculate means and standard deviations
            double[] means = new double[cols];
            double[] stds = new double[cols];
            
            for (int j = 0; j < cols; j++) {
                double sum = 0;
                for (double[] row : data) {
                    sum += row[j];
                }
                means[j] = sum / data.length;
            }
            
            for (int j = 0; j < cols; j++) {
                double sumSquares = 0;
                for (double[] row : data) {
                    sumSquares += (row[j] - means[j]) * (row[j] - means[j]);
                }
                stds[j] = Math.sqrt(sumSquares / data.length);
            }
            
            // Filter outliers
            for (double[] row : data) {
                boolean isOutlier = false;
                for (int j = 0; j < cols; j++) {
                    if (stds[j] > 0) {
                        double zScore = Math.abs((row[j] - means[j]) / stds[j]);
                        if (zScore > threshold) {
                            isOutlier = true;
                            break;
                        }
                    }
                }
                if (!isOutlier) {
                    cleanedData.add(row.clone());
                }
            }
            
            return cleanedData.toArray(new double[0][]);
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " -> Outliers Removed(z>" + threshold + ")";
        }
    }
    
    static class LogTransformDecorator extends FeatureDecorator {
        LogTransformDecorator(DataProcessor processor) {
            super(processor);
        }
        
        @Override
        public double[][] process(double[][] data) {
            double[][] processedData = super.process(data);
            return logTransform(processedData);
        }
        
        private double[][] logTransform(double[][] data) {
            int rows = data.length;
            int cols = data[0].length;
            double[][] transformed = new double[rows][cols];
            
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    // Add 1 to handle zero values, then take natural log
                    transformed[i][j] = Math.log(Math.abs(data[i][j]) + 1);
                }
            }
            
            return transformed;
        }
        
        @Override
        public String getDescription() {
            return super.getDescription() + " -> Log Transformed";
        }
    }
    
    public static void main(String[] args) {
        // Sample dataset
        double[][] rawData = {
            {100, 5, 1},
            {200, 10, 2},
            {150, 7, 1.5},
            {1000, 50, 10}, // Outlier
            {180, 8, 1.8},
            {220, 12, 2.2}
        };
        
        System.out.println("Feature Engineering with Decorator Pattern\n");
        
        // Create processing pipeline
        DataProcessor processor = new BaseDataProcessor();
        
        System.out.println("Original data shape: " + rawData.length + "x" + rawData[0].length);
        System.out.println("Pipeline: " + processor.getDescription());
        printDataSample(processor.process(rawData));
        
        // Add outlier removal
        processor = new OutlierRemovalDecorator(processor, 2.0);
        System.out.println("\nAfter outlier removal:");
        System.out.println("Pipeline: " + processor.getDescription());
        double[][] cleanedData = processor.process(rawData);
        System.out.println("Data shape: " + cleanedData.length + "x" + cleanedData[0].length);
        printDataSample(cleanedData);
        
        // Add log transformation
        processor = new LogTransformDecorator(processor);
        System.out.println("\nAfter log transformation:");
        System.out.println("Pipeline: " + processor.getDescription());
        printDataSample(processor.process(rawData));
        
        // Add standardization
        processor = new StandardizationDecorator(processor);
        System.out.println("\nAfter standardization:");
        System.out.println("Pipeline: " + processor.getDescription());
        printDataSample(processor.process(rawData));
        
        // Add polynomial features
        processor = new PolynomialFeaturesDecorator(processor, 2);
        System.out.println("\nAfter polynomial features:");
        System.out.println("Pipeline: " + processor.getDescription());
        double[][] finalData = processor.process(rawData);
        System.out.println("Final data shape: " + finalData.length + "x" + finalData[0].length);
        printDataSample(finalData);
    }
    
    private static void printDataSample(double[][] data) {
        System.out.println("Sample rows:");
        for (int i = 0; i < Math.min(3, data.length); i++) {
            System.out.print("  [");
            for (int j = 0; j < Math.min(5, data[i].length); j++) {
                System.out.printf("%.3f", data[i][j]);
                if (j < Math.min(4, data[i].length - 1)) System.out.print(", ");
            }
            if (data[i].length > 5) System.out.print(", ...");
            System.out.println("]");
        }
        if (data.length > 3) System.out.println("  ...");
        System.out.println();
    }
}
