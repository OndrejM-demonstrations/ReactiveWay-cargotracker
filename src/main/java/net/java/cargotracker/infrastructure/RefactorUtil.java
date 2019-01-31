package net.java.cargotracker.infrastructure;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public final class RefactorUtil {
    private RefactorUtil() {
    }
    
    public static <RESULT_TYPE> RESULT_TYPE stageToResult(CompletionStage<RESULT_TYPE> futureResult) throws RuntimeException {
        try {
            return futureResult.toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new RuntimeException(ex);
        }
    }
}
