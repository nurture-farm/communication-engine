/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.communication.engine.grpc;

import com.google.inject.Inject;
import farm.nurture.core.contracts.communication.engine.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GrpcService extends CommunicationEnginePlatformGrpc.CommunicationEnginePlatformImplBase {

    @Inject
    private CommunicationEngine communicationEngine;

    public void optInUser(OptInRequest request, StreamObserver<OptInRespone> responseObserver) {

        OptInRespone response = communicationEngine.optInUser(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void searchMessageAcknowledgements(MessageAcknowledgementRequest request, StreamObserver<MessageAcknowledgementResponse> responseObserver) {

        MessageAcknowledgementResponse response = communicationEngine.searchMessageAcknowledgements(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void addTemplate(AddTemplateRequest request, StreamObserver<AddTemplateResponse> responseObserver) {

        AddTemplateResponse response = communicationEngine.addTemplate(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void optOutUser(OptOutRequest request, StreamObserver<OptOutResponse> responseObserver) {

        OptOutResponse response = communicationEngine.optOutUser(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void getAllTemplate(GetAllTemplateRequest request, StreamObserver<GetAllTemplateResponse> responseObserver) {
        GetAllTemplateResponse response = communicationEngine.getAllTemplate(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    public void updateTemplate(TemplateUpdateRequest request, StreamObserver<AddTemplateResponse> responseObserver) {
        AddTemplateResponse response = communicationEngine.updateTemplate(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void activateTemplate(ActivationRequest request, StreamObserver<ActivationResponse> responseObserver) {
        ActivationResponse response = communicationEngine.activateTemplate(request);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
