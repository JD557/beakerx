/*
 *  Copyright 2017 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *import static org.assertj.core.api.Assertions.assertThat;
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.twosigma.beakerx.kernel.magic.command.functionality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twosigma.beakerx.kernel.KernelFunctionality;
import com.twosigma.beakerx.kernel.PythonEntryPoint;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandExecutionParam;
import com.twosigma.beakerx.kernel.magic.command.MagicCommandFunctionality;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicCommandOutcomeItem;
import com.twosigma.beakerx.kernel.magic.command.outcome.MagicKernelResponse;
import com.twosigma.beakerx.message.Header;
import com.twosigma.beakerx.message.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PythonMagicCommand implements MagicCommandFunctionality {

    public static final String PYTHON = "%%python";
    private KernelFunctionality kernel;

    public PythonMagicCommand(KernelFunctionality kernel) {
        this.kernel = kernel;
    }

    @Override
    public MagicCommandOutcomeItem execute(MagicCommandExecutionParam param) {
        PythonEntryPoint pep = kernel.getPythonEntryPoint();
        String codeBlock = param.getCommandCodeBlock();
        pep.evaluate(codeBlock);
        pep.getShellMsg();
        List<Message> messages = new ArrayList<>();
        while (true) {
            String iopubMsg = pep.getIopubMsg();
            if (iopubMsg.equals("null")) break;
            try {
                messages.add(parseMessage(iopubMsg));
            } catch (IOException e) {
                return new MagicKernelResponse(MagicCommandOutcomeItem.Status.ERROR, messages);
            }
        }
        return new MagicKernelResponse(MagicCommandOutcomeItem.Status.OK, messages);
    }

    public Message parseMessage(String stringJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Message msg = new Message();
        JsonNode json = mapper.readTree(stringJson);
        msg.setContent(mapper.convertValue(json.get("content"), Map.class));
        msg.setMetadata(mapper.convertValue(json.get("metadata"), Map.class));
        msg.setBuffers(mapper.convertValue(json.get("buffers"), List.class));
        List<byte[]> identities = mapper.convertValue(json.get("comm_id"), List.class);
        msg.setIdentities(identities == null ? new ArrayList<>() : identities);
        msg.setHeader(mapper.convertValue(json.get("header"), Header.class));
        return msg;
    }

    @Override
    public String getMagicCommandName() {
        return PYTHON;
    }
}
