package uk.co.optimisticpanda.tryplay;

import static org.assertj.core.api.StrictAssertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.co.optimisticpanda.tryplay.Try.ThrowsConsumer;

@RunWith(MockitoJUnitRunner.class)
public class TryTest {

	@Mock
	private ThrowsConsumer<Exception, RuntimeException> exceptionConsumer = (ex) -> {};
	@Mock
	private ThrowsConsumer<IllegalStateException, RuntimeException> illegalStateExceptionConsumer = (ex) -> {};
	@Mock
	private ThrowsConsumer<IOException, RuntimeException> ioExceptionConsumer = (ex) -> {};
	
	@Test
	public void mapSuccessCall() {
		Try<String> helloWorld = Try.of("Hello").map(v -> v + " World!");
		assertThat(helloWorld.toOptional()).isPresent();
		assertThat(helloWorld.toOptional()).contains("Hello World!");
	}

	@Test
	public void mapSuccessCallChainingThroughTypes() {
		Try<String> helloWorld = Try.of("Hello").map(v -> v.length()).map(String::valueOf);
		assertThat(helloWorld.toOptional()).isPresent();
		assertThat(helloWorld.toOptional()).contains("5");
	}
	
	@Test
	public void flatMapSuccessCall() {
		Try<Integer> helloWorldLength = Try.of("Hello").flatmap(v -> Try.of(v.length()));
		assertThat(helloWorldLength.toOptional()).isPresent();
		assertThat(helloWorldLength.toOptional()).contains(5);
	}

	@Test
	public void errorIsEmpty() {
		Try<Integer> tryError = Try.of("Hello").flatmap(v -> {
			throw new RuntimeException("wahA!:" + v);	
		});
		
		assertThat(tryError.toOptional()).isEmpty();
	}
	
	@Test
	public void checkSuccessFlags() {
		Try<String> success = Try.of("value");
		assertThat(success.isSuccess()).isTrue();
		assertThat(success.isFail()).isFalse();
	}
	
	@Test
	public void checkFailFlags() {
		Try<String> fail = Try.of(new RuntimeException("sdd"));
		assertThat(fail.isSuccess()).isFalse();
		assertThat(fail.isFail()).isTrue();
	}
	
	@Test
	public void failsOnFirstError() {
		Try<Integer> helloWorldLength = Try.of("Hello").flatmap(v -> {
			throw new RuntimeException("wahA!:" + v);	
		}).flatmap(v -> {
			throw new RuntimeException("wahB!:" + v);
		});
		
		helloWorldLength.onAnyFailure(exceptionConsumer);
		
		verifyGenericExceptionConsumerCalledWithMessage("wahA!:Hello");
	}

	@Test
	public void specificErrorsCanBeHandled() {
		Try<Integer> helloWorldLength = Try.of("Hello").flatmap(v -> {
			throw new IllegalStateException("wahA!:" + v);	
		});
		
		helloWorldLength.onFailure(IllegalStateException.class, illegalStateExceptionConsumer);

		verifyIllegalStateExceptionConsumerCalled();
	}
	
	@Test
	public void nonSpecifiedErrorsAreIgnored() {
		Try<Integer> helloWorldLength = Try.of("Hello").flatmap(v -> {
			throw new IOException("wahA!:" + v);	
		});
		
		helloWorldLength.onFailure(IllegalStateException.class, illegalStateExceptionConsumer);
		
		verify(illegalStateExceptionConsumer, never()).accept(any());
	}

	@Test
	public void canThrowCheckedExceptionsOnFailure() {
		Try<Integer> helloWorldLength = Try.of("Hello").flatmap(v -> {
			throw new IllegalStateException("wahA!:" + v);	
		});
		
		assertThatThrownBy(() -> 
			helloWorldLength.onFailure(IllegalStateException.class, 
					ex -> { throw new IOException("error:" + ex.getMessage());}))
			.isInstanceOf(IOException.class).hasMessage("error:wahA!:Hello");
		
		verify(illegalStateExceptionConsumer, never()).accept(any());
	}

	@Test
	public void canThrowCheckedExceptionsOnSuccess() {
		Try<String> helloWorldLength = Try.of("Hello");
		
		assertThatThrownBy(() -> 
			helloWorldLength.onSuccess(v -> { throw new IOException("error:" + v);}))
			.isInstanceOf(IOException.class).hasMessage("error:Hello");
		
		verify(illegalStateExceptionConsumer, never()).accept(any());
	}
	
	@Test
	public void specifiedErrorsCanBeHandledMoreThanOnce() {
		Try<Integer> helloWorldLength = Try.of("Hello").flatmap(v -> {
			throw new IllegalStateException("wahA!:" + v);	
		});
		
		helloWorldLength.onFailure(IllegalStateException.class, illegalStateExceptionConsumer);
		helloWorldLength.onFailure(IOException.class, ioExceptionConsumer);
		helloWorldLength.onAnyFailure(exceptionConsumer);

		verifyGenericExceptionConsumerCalledWithMessage("wahA!:Hello");
		verifyIllegalStateExceptionConsumerCalled();
		verify(ioExceptionConsumer, never()).accept(any());
	}

	private void verifyIllegalStateExceptionConsumerCalled() {
		ArgumentCaptor<IllegalStateException> captor = ArgumentCaptor.forClass(IllegalStateException.class);
		verify(illegalStateExceptionConsumer).accept(captor.capture());
		assertThat(captor.getValue())
							.isInstanceOf(RuntimeException.class)
							.hasMessage("wahA!:Hello");
	}

	private void verifyGenericExceptionConsumerCalledWithMessage(String message) {
		ArgumentCaptor<Exception> captor = ArgumentCaptor.forClass(Exception.class);
		verify(exceptionConsumer).accept(captor.capture());
		assertThat(captor.getValue())
							.isInstanceOf(RuntimeException.class)
							.hasMessage(message);
	}
}
