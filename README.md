# try-play
Having a go at implementing Try. 

This is a relatively minamilist implementation which relies on mapping to optional for most operations. 

```java

  @Test
  public void mapSuccessCallChainingThroughTypes() {
    Try<String> helloWorld = Try.of("Hello").map(v -> v.length()).map(String::valueOf);
    assertThat(helloWorld.toOptional()).isPresent();
    assertThat(helloWorld.toOptional()).contains("5");
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

```

