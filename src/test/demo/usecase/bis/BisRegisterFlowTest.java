/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package demo.usecase.bis;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import conf.AkkaStreamingConfiguration;
import conf.InProcessPoolConfiguration;
import rx.Observable;
import rx.functions.Action1;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;
import stream.ReactStreams;
import stream.StreamProcessingSupport;
import stream.impl.NamedStreamId;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AkkaStreamingConfiguration.class,
        InProcessPoolConfiguration.class }, loader = AnnotationConfigContextLoader.class)
public class BisRegisterFlowTest extends StreamProcessingSupport {

    private static final int USER_PERMIT_1_A_AND_B_TRUE = 65537;
    private static final int SOURCE_STREAM_ELEMENTS = 20;
    private static final NamedStreamId<Integer> SOURCE_ID = new NamedStreamId<>("BisUserPermitsStream");

    @Before
    public void setup() {
        provide(ReactStreams.fromRx(Observable.from(someValues()).observeOn(Schedulers.newThread())
                .delay(10, TimeUnit.MILLISECONDS).limit(SOURCE_STREAM_ELEMENTS).map(l -> Long.valueOf(l).intValue())))
                        .as(SOURCE_ID);
    }

    @Test
    public void test() throws InterruptedException {

        CountDownLatch sync = new CountDownLatch(1);

        ReactStreams.rxFrom(discover(SOURCE_ID)).flatMap(this::mapBitsToUserPermits)
                .groupBy(permit -> permit.getPermitId())
                .doOnCompleted(sync::countDown).subscribe(this::provideStreams);

        sync.await();

        Observable<UserPermit> userPermit10AStream = ReactStreams
                .rxFrom(discover(new NamedStreamId<>(RedundantPermitId.USER_PERMIT_1_A.toString())));
        Observable<UserPermit> userPermit10BStream = ReactStreams
                .rxFrom(discover(new NamedStreamId<>(RedundantPermitId.USER_PERMIT_1_B.toString())));

        Observable.zip(userPermit10AStream, userPermit10BStream, (userPermitA, userPermitB) -> {
            return userPermitA.isGiven() & userPermitB.isGiven();
        }).subscribe(value -> System.out.println("User Permit 1 : " + value));
    }

    private Observable<UserPermit> mapBitsToUserPermits(Integer register) {
        List<UserPermit> bitList = new ArrayList<>(32);
        for (RedundantPermitId permitId : RedundantPermitId.values()) {
            boolean testBit = BigInteger.valueOf(register).testBit(permitId.offset);
            bitList.add(new UserPermit(permitId, testBit));
        }
        return Observable.from(bitList);
    }

    private void provideStreams(GroupedObservable<RedundantPermitId, UserPermit> groupedObservable) {
        RedundantPermitId key = groupedObservable.getKey();
        provide(ReactStreams.fromRx(groupedObservable)).as(new NamedStreamId<>(key.toString()));
    }

    private List<Integer> someValues() {
        ArrayList<Integer> values = new ArrayList<>();
        for (int i = 0; i < SOURCE_STREAM_ELEMENTS; ++i) {
            values.add(new Random().nextBoolean() ? 0 : USER_PERMIT_1_A_AND_B_TRUE);
        }
        return values;
    }

}
