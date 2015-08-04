package uk.co.optimisticpanda.tryplay;
import java.util.Optional;

public abstract class Try<R> {

	public static <R> Try<R> tryTo(ThrowsSupplier<R> supplier) {
		try {
			return new Success<R>(supplier.get());
		} catch (Exception e) {
			return new Fail<R>(e);
		}
	}

	public static <R> Try<R> of(Exception ex) {
		return new Fail<R>(ex);
	}
	
	public static <R> Try<R> of(R value) {
		return new Success<R>(value);
	}
	
	public abstract boolean isSuccess();
	
	public abstract boolean isFail();
	
	public abstract Optional<R> toOptional();
	
	public abstract <S> Try<S> flatmap(ThrowsFunction<R, Try<S>> func);

	public abstract <S> Try<S> map(ThrowsFunction<R, S> func);
	
	public abstract <X extends Exception> void onSuccess(ThrowsConsumer<R, X> consumer) throws X;
	
	public abstract <C extends Exception, X extends Exception> void onFailure(Class<C> clazz, ThrowsConsumer<C, X> consumer) throws X;
	public abstract <X extends Exception> void onAnyFailure(ThrowsConsumer<Exception, X> consumer) throws X;
	
	private static class Success<R> extends Try<R> {

		private R value; 
		
		private Success(R value) {
			this.value = value;
		}

		@Override
		public boolean isSuccess() {
			return true;
		}
		
		@Override
		public boolean isFail() {
			return false;
		}
		
		@Override
		public Optional<R> toOptional() {
			return Optional.of(value);
		}
		
		@Override
		public <S> Try<S> flatmap(ThrowsFunction<R, Try<S>> func){
			try {
				return func.apply(value);
			} catch (Exception exception) {
				return new Fail<S>(exception);
			}
		}
		
		@Override
		public <S> Try<S> map(ThrowsFunction<R, S> func){
			try {
				return new Success<S>(func.apply(value));
			} catch (Exception exception) {
				return new Fail<S>(exception);
			}
		}
		
		@Override
		public <X extends Exception> void onSuccess(ThrowsConsumer<R, X> consumer) throws X {
			consumer.accept(value);
		}
		
		@Override
		public <C extends Exception, X extends Exception> void onFailure(Class<C> clazz, ThrowsConsumer<C, X> consumer) throws X {
		}

		@Override
		public <X extends Exception> void onAnyFailure(ThrowsConsumer<Exception, X> consumer) throws X {
		}
	}

	private static class Fail<R> extends Try<R> {

		private Exception error; 
		
		private Fail(Exception error) {
			this.error = error;
		}

		@Override
		public boolean isSuccess() {
			return false;
		}
		
		@Override
		public boolean isFail() {
			return true;
		}
		
		@Override
		public Optional<R> toOptional() {
			return Optional.empty();
		}
		
		@Override
		public <S> Try<S> flatmap(ThrowsFunction<R, Try<S>> func){
			return new Fail<S>(error);
		}
		
		@Override
		public <S> Try<S> map(ThrowsFunction<R, S> func){
			return new Fail<S>(error);
		}

		@Override
		public <X extends Exception> void onSuccess(ThrowsConsumer<R, X> consumer) throws X {
		}

		@Override
		@SuppressWarnings("unchecked")
		public <C extends Exception, X extends Exception> void onFailure(Class<C> clazz, ThrowsConsumer<C, X> consumer) throws X {
			if (clazz.isInstance(error)) {
				consumer.accept((C)error);
			}
		}

		@Override
		public <X extends Exception> void onAnyFailure(ThrowsConsumer<Exception, X> consumer) throws X {
			consumer.accept(error);
		}
		
	}

	@FunctionalInterface
	public interface ThrowsFunction<T, R> {
		public R apply(T value) throws Exception;
	}
	
	@FunctionalInterface
	public interface ThrowsSupplier<R> {
		public R get() throws Exception;
	}
	
	@FunctionalInterface
	public interface ThrowsConsumer<R, X extends Exception> {
		public void accept(R value) throws X;
	}
	
	
}
