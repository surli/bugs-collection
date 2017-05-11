#
# ovirt-engine-setup -- ovirt engine setup
# Copyright (C) 2013-2016 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#


"""VMConsole proxy plugin."""


from otopi import plugin
from otopi import util

from ovirt_engine_setup import constants as osetupcons
from ovirt_engine_setup.vmconsole_proxy_helper import constants as ovmpcons


@util.export
class Plugin(plugin.PluginBase):
    """vmconsole helper plugin."""

    @plugin.event(
        stage=plugin.Stages.STAGE_INIT,
    )
    def _init(self):
        self.environment.setdefault(
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_STOP_NEEDED,
            False
        )

    @plugin.event(
        stage=plugin.Stages.STAGE_CUSTOMIZATION,
        after=(
            osetupcons.Stages.REMOVE_CUSTOMIZATION_COMMON,
        ),
    )
    def _customization(self):
        if self.environment[
            ovmpcons.ConfigEnv.VMCONSOLE_PROXY_CONFIG
        ]:
            self.environment[
                ovmpcons.ConfigEnv.VMCONSOLE_PROXY_STOP_NEEDED
            ] = True

    @plugin.event(
        stage=plugin.Stages.STAGE_MISC,
        condition=lambda self: (
            not self.environment[osetupcons.CoreEnv.DEVELOPER_MODE] and
            self.environment[ovmpcons.ConfigEnv.VMCONSOLE_PROXY_STOP_NEEDED]
        ),
    )
    def _misc(self):
        if self.services.exists(
            name=ovmpcons.Const.VMCONSOLE_PROXY_SERVICE_NAME
        ):
            self.services.startup(
                name=ovmpcons.Const.VMCONSOLE_PROXY_SERVICE_NAME,
                state=False,
            )


# vim: expandtab tabstop=4 shiftwidth=4
