/*
 * Copyright (c) 2013, Ford Motor Company
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the Ford Motor Company nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include "can_cooperation/policy_helper.h"
#include "can_cooperation/can_module.h"
#include "can_cooperation/can_app_extension.h"
#include "application_manager/application.h"

namespace can_cooperation {

void PolicyHelper::OnRSDLFunctionalityAllowing(bool allowed) {
  CANModule::instance()->service()->SetRemoteControl(allowed);
}

void PolicyHelper::SetPrimaryDevice(const uint32_t device_handle,
                                    const std::string& input) {
  CANModule::instance()->service()->SetPrimaryDevice(device_handle, input);

  application_manager::AppExtensionUID module_id = CANModule::instance()->GetModuleID();
  std::vector<application_manager::ApplicationSharedPtr> applications =
    CANModule::instance()->service()->GetApplications(module_id);

  for (size_t i = 0; i < applications.size(); ++i) {
    CANAppExtensionPtr extension =
      application_manager::AppExtensionPtr::static_pointer_cast<CANAppExtension>(
        applications[i]->QueryInterface(module_id));
    DCHECK(extension);
    bool is_driver = (applications[i]->device() == device_handle);
    extension->set_is_on_driver_device(is_driver);
  }
}

}  // namespace can_cooperation
