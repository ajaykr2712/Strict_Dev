public class SagaPatternExample {
    interface SagaStep { boolean process(Context ctx); void compensate(Context ctx); }
    static class Context { boolean paymentDone, inventoryReserved, shipped; }

    static class PaymentStep implements SagaStep {
        public boolean process(Context ctx){ System.out.println("Taking payment"); ctx.paymentDone = true; return true; }
        public void compensate(Context ctx){ if (ctx.paymentDone){ System.out.println("Refunding payment"); ctx.paymentDone = false; } }
    }

    static class InventoryStep implements SagaStep {
        private final boolean simulateFailure;
        InventoryStep(boolean fail){ this.simulateFailure = fail; }
        public boolean process(Context ctx){ System.out.println("Reserving inventory"); if (simulateFailure){ System.out.println("Inventory reservation failed"); return false; } ctx.inventoryReserved = true; return true; }
        public void compensate(Context ctx){ if (ctx.inventoryReserved){ System.out.println("Releasing inventory"); ctx.inventoryReserved = false; } }
    }

    static class ShippingStep implements SagaStep {
        public boolean process(Context ctx){ System.out.println("Creating shipment"); ctx.shipped = true; return true; }
        public void compensate(Context ctx){ if (ctx.shipped){ System.out.println("Cancel shipment"); ctx.shipped = false; } }
    }

    static class Saga {
        private final java.util.List<SagaStep> steps = new java.util.ArrayList<>();
        Saga add(SagaStep step){ steps.add(step); return this; }
        boolean execute(){
            Context ctx = new Context();
            java.util.Deque<SagaStep> executed = new java.util.ArrayDeque<>();
            for (SagaStep s : steps){
                if (s.process(ctx)){
                    executed.push(s);
                } else {
                    while (!executed.isEmpty()) executed.pop().compensate(ctx);
                    System.out.println("Saga failed and compensated");
                    return false;
                }
            }
            System.out.println("Saga completed successfully");
            return true;
        }
    }

    public static void main(String[] args){
        System.out.println("Successful run:");
        new Saga().add(new PaymentStep()).add(new InventoryStep(false)).add(new ShippingStep()).execute();
        System.out.println("\nFailure run:");
        new Saga().add(new PaymentStep()).add(new InventoryStep(true)).add(new ShippingStep()).execute();
    }
}
