import java.util.*;

// K-Means Clustering Algorithm
public class KMeansClustering {
    static class Point {
        final double[] coordinates;
        int cluster = -1;
        
        Point(double... coordinates) {
            this.coordinates = coordinates.clone();
        }
        
        double distanceTo(Point other) {
            double sum = 0;
            for (int i = 0; i < coordinates.length; i++) {
                double diff = coordinates[i] - other.coordinates[i];
                sum += diff * diff;
            }
            return Math.sqrt(sum);
        }
        
        @Override
        public String toString() {
            return Arrays.toString(coordinates) + " (cluster=" + cluster + ")";
        }
    }
    
    static class Centroid {
        double[] coordinates;
        
        Centroid(int dimensions) {
            this.coordinates = new double[dimensions];
        }
        
        Centroid(Point point) {
            this.coordinates = point.coordinates.clone();
        }
        
        void updateFromPoints(List<Point> points) {
            if (points.isEmpty()) return;
            
            Arrays.fill(coordinates, 0);
            for (Point p : points) {
                for (int i = 0; i < coordinates.length; i++) {
                    coordinates[i] += p.coordinates[i];
                }
            }
            
            for (int i = 0; i < coordinates.length; i++) {
                coordinates[i] /= points.size();
            }
        }
        
        Point asPoint() {
            return new Point(coordinates);
        }
        
        @Override
        public String toString() {
            return "Centroid" + Arrays.toString(coordinates);
        }
    }
    
    private final int k;
    private final int maxIterations;
    private final double tolerance;
    private List<Centroid> centroids;
    private boolean converged = false;
    
    public KMeansClustering(int k, int maxIterations, double tolerance) {
        this.k = k;
        this.maxIterations = maxIterations;
        this.tolerance = tolerance;
    }
    
    public void fit(List<Point> points) {
        if (points.size() < k) {
            throw new IllegalArgumentException("Number of points must be >= k");
        }
        
        initializeCentroids(points);
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            System.out.println("Iteration " + (iteration + 1));
            
            // Assign points to clusters
            assignPointsToClusters(points);
            
            // Update centroids
            List<Centroid> newCentroids = updateCentroids(points);
            
            // Check for convergence
            if (hasConverged(centroids, newCentroids)) {
                System.out.println("Converged after " + (iteration + 1) + " iterations");
                converged = true;
                break;
            }
            
            centroids = newCentroids;
            printClusteringState(points);
        }
        
        if (!converged) {
            System.out.println("Did not converge within " + maxIterations + " iterations");
        }
    }
    
    private void initializeCentroids(List<Point> points) {
        centroids = new ArrayList<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        // K-means++ initialization for better initial centroids
        // First centroid is chosen randomly
        centroids.add(new Centroid(points.get(random.nextInt(points.size()))));
        
        for (int i = 1; i < k; i++) {
            double[] distances = new double[points.size()];
            double totalDistance = 0;
            
            // Calculate distance to nearest centroid for each point
            for (int j = 0; j < points.size(); j++) {
                double minDistance = Double.MAX_VALUE;
                for (Centroid centroid : centroids) {
                    double distance = points.get(j).distanceTo(centroid.asPoint());
                    minDistance = Math.min(minDistance, distance);
                }
                distances[j] = minDistance * minDistance; // Squared distance
                totalDistance += distances[j];
            }
            
            // Choose next centroid with probability proportional to squared distance
            double target = random.nextDouble() * totalDistance;
            double cumulative = 0;
            for (int j = 0; j < points.size(); j++) {
                cumulative += distances[j];
                if (cumulative >= target) {
                    centroids.add(new Centroid(points.get(j)));
                    break;
                }
            }
        }
        
        System.out.println("Initialized " + k + " centroids using K-means++");
    }
    
    private void assignPointsToClusters(List<Point> points) {
        for (Point point : points) {
            double minDistance = Double.MAX_VALUE;
            int bestCluster = 0;
            
            for (int i = 0; i < centroids.size(); i++) {
                double distance = point.distanceTo(centroids.get(i).asPoint());
                if (distance < minDistance) {
                    minDistance = distance;
                    bestCluster = i;
                }
            }
            
            point.cluster = bestCluster;
        }
    }
    
    private List<Centroid> updateCentroids(List<Point> points) {
        List<Centroid> newCentroids = new ArrayList<>();
        
        for (int i = 0; i < k; i++) {
            List<Point> clusterPoints = new ArrayList<>();
            for (Point point : points) {
                if (point.cluster == i) {
                    clusterPoints.add(point);
                }
            }
            
            Centroid newCentroid = new Centroid(points.get(0).coordinates.length);
            if (!clusterPoints.isEmpty()) {
                newCentroid.updateFromPoints(clusterPoints);
            } else {
                // If cluster is empty, keep the old centroid or reinitialize
                newCentroid = new Centroid(centroids.get(i).asPoint());
            }
            newCentroids.add(newCentroid);
        }
        
        return newCentroids;
    }
    
    private boolean hasConverged(List<Centroid> oldCentroids, List<Centroid> newCentroids) {
        for (int i = 0; i < oldCentroids.size(); i++) {
            Point oldPoint = oldCentroids.get(i).asPoint();
            Point newPoint = newCentroids.get(i).asPoint();
            if (oldPoint.distanceTo(newPoint) > tolerance) {
                return false;
            }
        }
        return true;
    }
    
    private void printClusteringState(List<Point> points) {
        System.out.println("Current centroids:");
        for (int i = 0; i < centroids.size(); i++) {
            System.out.println("  Cluster " + i + ": " + centroids.get(i));
        }
        
        // Count points in each cluster
        int[] clusterCounts = new int[k];
        for (Point point : points) {
            if (point.cluster >= 0) {
                clusterCounts[point.cluster]++;
            }
        }
        
        System.out.println("Cluster sizes: " + Arrays.toString(clusterCounts));
        System.out.println();
    }
    
    public List<Centroid> getCentroids() {
        return new ArrayList<>(centroids);
    }
    
    public double calculateWithinClusterSumOfSquares(List<Point> points) {
        double totalWSS = 0;
        
        for (int cluster = 0; cluster < k; cluster++) {
            double clusterWSS = 0;
            Centroid centroid = centroids.get(cluster);
            
            for (Point point : points) {
                if (point.cluster == cluster) {
                    double distance = point.distanceTo(centroid.asPoint());
                    clusterWSS += distance * distance;
                }
            }
            
            totalWSS += clusterWSS;
        }
        
        return totalWSS;
    }
    
    public static void main(String[] args) {
        System.out.println("K-Means Clustering Example\n");
        
        // Create sample dataset - three natural clusters
        List<Point> points = Arrays.asList(
            // Cluster 1: around (2, 2)
            new Point(1.5, 2.1), new Point(2.2, 1.8), new Point(1.8, 2.5), new Point(2.1, 2.2),
            // Cluster 2: around (7, 8)
            new Point(7.2, 8.1), new Point(6.8, 7.5), new Point(7.5, 8.3), new Point(7.1, 7.9),
            // Cluster 3: around (3, 7)
            new Point(2.8, 6.9), new Point(3.2, 7.3), new Point(3.1, 6.7), new Point(2.9, 7.1),
            // Some additional points
            new Point(1.9, 1.9), new Point(7.0, 8.0), new Point(3.0, 7.0)
        );
        
        System.out.println("Original points:");
        for (Point p : points) {
            System.out.println("  " + p);
        }
        System.out.println();
        
        // Run K-means with k=3
        KMeansClustering kmeans = new KMeansClustering(3, 100, 0.01);
        kmeans.fit(points);
        
        System.out.println("Final clustering results:");
        for (Point p : points) {
            System.out.println("  " + p);
        }
        
        System.out.println("\nFinal centroids:");
        for (int i = 0; i < kmeans.getCentroids().size(); i++) {
            System.out.println("  Cluster " + i + ": " + kmeans.getCentroids().get(i));
        }
        
        double wss = kmeans.calculateWithinClusterSumOfSquares(points);
        System.out.printf("%nWithin-cluster sum of squares: %.3f%n", wss);
        
        // Demonstrate elbow method for finding optimal k
        System.out.println("\nElbow method for optimal k:");
        for (int testK = 1; testK <= Math.min(6, points.size()); testK++) {
            KMeansClustering testKmeans = new KMeansClustering(testK, 100, 0.01);
            // Reset clusters for fresh run
            for (Point p : points) {
                p.cluster = -1;
            }
            testKmeans.fit(points);
            double testWSS = testKmeans.calculateWithinClusterSumOfSquares(points);
            System.out.printf("k=%d: WSS=%.3f%n", testK, testWSS);
        }
    }
}
