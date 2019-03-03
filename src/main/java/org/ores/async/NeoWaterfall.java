package org.ores.async;

import java.util.*;
import static org.ores.async.NeoWaterfallI.AsyncTask;
import static org.ores.async.NeoWaterfallI.AsyncCallback;

/**
 * See <a href="http://google.com">http://google.com</a>
 */
class NeoWaterfall {
  
  @SuppressWarnings("Duplicates")
  static <T, E> void Waterfall(
    final List<AsyncTask<T, E>> tasks,
    final Asyncc.IAsyncCallback<HashMap<String, Object>, E> f) {
    
    final HashMap<String, Object> results = new HashMap<>();
    
    if (tasks.size() < 1) {
      f.done(null, results);
      return;
    }
  
    final CounterLimit c = new CounterLimit(1);
    final ShortCircuit s = new ShortCircuit();
    
    WaterfallInternal(tasks, results, s, c, f);
    NeoUtils.handleSameTickCall(s);
    
  }
  
  @SuppressWarnings("Duplicates")
  private static <T, E> void WaterfallInternal(
    final List<AsyncTask<T, E>> tasks,
    final HashMap<String, Object> results,
    final ShortCircuit s,
    final CounterLimit c,
    final Asyncc.IAsyncCallback<HashMap<String, Object>, E> f) {
    
    final int startedCount = c.getStartedCount();
    
    if (startedCount >= tasks.size()) {
//      f.done(null, results);
      return;
    }
    
    final AsyncTask<T, E> t = tasks.get(startedCount);
    final var taskRunner = new AsyncCallback<T, E>(s, results) {
      
      protected void doneInternal(final Asyncc.Marker done, final E e, final Map.Entry<String, T> m) {
        
        synchronized (this.cbLock) {
          
          if (this.isFinished()) {
            new Error("Warning: Callback fired more than once.").printStackTrace(System.err);
            return;
          }
          
          this.setFinished(true);
          c.incrementFinished();
          
          if (s.isShortCircuited()) {
            return;
          }
          
        }
        
        if (m != null) {
          results.put(m.getKey(), m.getValue());
        }
        
        if (e != null) {
          s.setShortCircuited(true);
          NeoUtils.fireFinalCallback(s, e, results, f);
          return;
        }
        
        if (c.getFinishedCount() == tasks.size()) {
          NeoUtils.fireFinalCallback(s, null, results, f);
          return;
        }
        
        WaterfallInternal(tasks, results, s, c, f);
        
      }
      
    };
    
    c.incrementStarted();
    
    try {
      t.run(taskRunner);
    } catch (Exception e) {
      s.setShortCircuited(true);
      NeoUtils.fireFinalCallback(s, e, results, f);
    }
    
  }
}
