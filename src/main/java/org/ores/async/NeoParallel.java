package org.ores.async;

import java.util.*;

class NeoParallel {
  
  @SuppressWarnings("Duplicates")
  static <T, E> void ParallelLimit(
    int limit,
    Map<String, Asyncc.AsyncTask<T, E>> tasks,
    Asyncc.IAsyncCallback<Map<String, T>, E> f) {
    
    Map<String, T> results = new HashMap<>();
    CounterLimit c = new CounterLimit(limit);
    ShortCircuit s = new ShortCircuit();
    
    Iterator<Map.Entry<String, Asyncc.AsyncTask<T, E>>> entries = tasks.entrySet().iterator();
//    Limit lim = new Limit(limit);
    
    Util.<T, E>RunMapLimit(entries, tasks, results, c, s, f);
    
  }
  
  @SuppressWarnings("Duplicates")
  static <T, E> void ParallelLimit(
    int limit,
    List<Asyncc.AsyncTask<T, E>> tasks,
    Asyncc.IAsyncCallback<List<T>, E> f) {
    
    ShortCircuit s = new ShortCircuit();
    List<T> results = new ArrayList<T>(Collections.<T>nCopies(tasks.size(), null));
    CounterLimit c = new CounterLimit(limit);
    
    Util.<T, E>RunTasksLimit(tasks, results, c, s, f);
    
  }
  
  @SuppressWarnings("Duplicates")
  static <T, E> void Parallel(Map<String, Asyncc.AsyncTask<T, E>> tasks, Asyncc.IAsyncCallback<Map<String, T>, E> f) {
    
    Map<String, T> results = new HashMap<>();
    CounterLimit c = new CounterLimit(Integer.MAX_VALUE);
    ShortCircuit s = new ShortCircuit();
    
    for (Map.Entry<String, Asyncc.AsyncTask<T, E>> entry : tasks.entrySet()) {
      
      final String key = entry.getKey();
      final var taskRunner = new Asyncc.AsyncCallback<T, E>(s) {
  
        @Override
        public void resolve(T v) {
          this.done(null, v);
        }
  
        @Override
        public void reject(E e) {
          this.done(e, null);
        }
  
        @Override
        public void done(E e, T v) {
    
          synchronized (this.cbLock) {
      
            if (this.isFinished()) {
              new Error("Warning: Callback fired more than once.").printStackTrace();
              return;
            }
      
            this.setFinished(true);
      
            if (s.isShortCircuited()) {
              return;
            }
      
            if (e != null) {
              s.setShortCircuited(true);
              f.done(e, Map.of());
              return;
            }
      
            c.incrementFinished();
            results.put(key, v);
      
            if (c.getFinishedCount() == tasks.size()) {
              f.done(null, results);
            }
      
          }
        }
      };
      
      try{
        entry.getValue().run(taskRunner);
      }
      catch(Exception e){
        s.setShortCircuited(true);
        f.done((E)e, results);
        return;
      }
      
      
    }
    
  }
  
  @SuppressWarnings("Duplicates")
  static <T, E> void Parallel(List<Asyncc.AsyncTask<T, E>> tasks, Asyncc.IAsyncCallback<List<T>, E> f) {
    
    List<T> results = new ArrayList<T>(Collections.<T>nCopies(tasks.size(), null));
    
    if (tasks.size() < 1) {
      f.done(null, results);
      return;
    }
    
    CounterLimit c = new CounterLimit(Integer.MAX_VALUE);
    ShortCircuit s = new ShortCircuit();
    
    for (int i = 0; i < tasks.size(); i++) {
      
      final int index = i;
      
      tasks.get(i).run(new Asyncc.AsyncCallback<T, E>(s) {
        
        @Override
        public void resolve(T v) {
          this.done(null, v);
        }
        
        @Override
        public void reject(E e) {
          this.done(e, null);
        }
        
        @Override
        public void done(E e, T v) {
          
          synchronized (this.cbLock) {
            
            if (this.isFinished()) {
              new Error("Warning: Callback fired more than once.").printStackTrace();
              return;
            }
            
            this.setFinished(true);
            
            if (s.isShortCircuited()) {
              return;
            }
            
            c.incrementFinished();
            results.set(index, v);
          }
          
          if (e != null) {
            s.setShortCircuited(true);
            f.done(e, Collections.emptyList());
            return;
          }
          
          if (c.getFinishedCount() == tasks.size()) {
            f.done(null, results);
          }
        }
      });
      
    }
    
  }
  
  
}
