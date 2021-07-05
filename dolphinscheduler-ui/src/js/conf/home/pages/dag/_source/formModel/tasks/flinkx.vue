/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
<template>
  <div class="flinkx-model">
    <m-list-box>
      <div slot="text">{{ $t('Run Mode') }}</div>
      <div slot="content">
        <x-radio-group v-model="deployMode">
          <x-radio :label="'local'" :disabled="isDetails"></x-radio>
          <x-radio :label="'standalone'" :disabled="isDetails"></x-radio>
          <x-radio :label="'yarn'" :disabled="isDetails"></x-radio>
          <x-radio :label="'yarnPre'" :disabled="isDetails"></x-radio>
        </x-radio-group>
      </div>
    </m-list-box>
    <m-list-box>
      <div slot="text">json</div>
      <div slot="content">
        <div class="form-mirror">
            <textarea
              id="code-json-mirror"
              name="code-json-mirror"
              style="opacity: 0;">
            </textarea>
        </div>
      </div>
    </m-list-box>
    <m-list-box>
      <div slot="text">{{ $t('Custom Parameters') }}</div>
      <div slot="content">
        <m-local-params
          ref="refLocalParams"
          @on-local-params="_onLocalParams"
          :udp-list="localParams"
          :hide="false">
        </m-local-params>
      </div>
    </m-list-box>
  </div>
</template>
<script>
import _ from 'lodash'
import i18n from '@/module/i18n'
import mListBox from './_source/listBox'
import mDatasource from './_source/datasource'
import mLocalParams from './_source/localParams'
import mStatementList from './_source/statementList'
import disabledState from '@/module/mixin/disabledState'
import mSelectInput from '../_source/selectInput'
import codemirror from '@/conf/home/pages/resource/pages/file/pages/_source/codemirror'

let editor
let jsonEditor

export default {
  name: 'datax',

  data() {
    return {
      deployMode: 'local',
      json: '',
      // Custom parameter
      localParams: [],
      customConfig: 1,
    }
  },
  mixins: [disabledState],
  props: {
    backfillItem: Object,
    createNodeId: Number
  },
  methods: {
    /**
     * return localParams
     */
    _onLocalParams(a) {
      this.localParams = a
    },
    /**
     * verification
     */
    _verification() {

      if (!jsonEditor.getValue()) {
        this.$message.warning(`${i18n.$t('Please enter a JSON Statement(required)')}`)
        return false
      }

      // localParams Subcomponent verification
      if (!this.$refs.refLocalParams._verifProp()) {
        return false
      }

      // storage
      this.$emit('on-params', {
        customConfig: this.customConfig,
        deployMode: this.deployMode,
        json: jsonEditor.getValue(),
        localParams: this.localParams
      })
      return true

    },
    /**
     * Processing code highlighting
     */
    _handlerJsonEditor() {
      this._destroyJsonEditor()

      // jsonEditor
      jsonEditor = codemirror('code-json-mirror', {
        mode: 'json',
        readOnly: this.isDetails
      })

      this.keypress = () => {
        if (!jsonEditor.getOption('readOnly')) {
          jsonEditor.showHint({
            completeSingle: false
          })
        }
      }

      // Monitor keyboard
      jsonEditor.on('keypress', this.keypress)

      jsonEditor.on('changes', () => {
        // this._cacheParams()
      })

      jsonEditor.setValue(this.json)

      return jsonEditor
    },
    _cacheParams() {
      this.$emit('on-cache-params', {
        deployMode: this.deployMode
      });
    },
    _destroyJsonEditor() {
      if (jsonEditor) {
        jsonEditor.toTextArea() // Uninstall
        jsonEditor.off($('.code-json-mirror'), 'keypress', this.keypress)
        jsonEditor.off($('.code-json-mirror'), 'changes', this.changes)
      }
    }
  },
  created() {
    let o = this.backfillItem

    // Non-null objects represent backfill
    if (!_.isEmpty(o)) {
      // backfill
      this.customConfig = 1
      this.enable = true
      this.deployMode = o.params.deployMode
      this.json = o.params.json || []
      this.localParams = o.params.localParams || ''
    }
  },
  mounted() {
    if (this.customConfig) {
      setTimeout(() => {
        this._handlerJsonEditor()
      }, 350)
    } else {
      setTimeout(() => {
        this._handlerEditor()
      }, 350)
    }
  },
  destroyed() {
    /**
     * Destroy the editor instance
     */
    if (editor) {
      editor.toTextArea() // Uninstall
      editor.off($('.code-sql-mirror'), 'keypress', this.keypress)
    }
    if (jsonEditor) {
      jsonEditor.toTextArea() // Uninstall
      jsonEditor.off($('.code-json-mirror'), 'keypress', this.keypress)
    }
  },
  watch: {
    //Watch the cacheParams
    cacheParams(val) {
      this._cacheParams();
    }
  },
  computed: {
    cacheParams() {
      return {
        deployMode: this.deployMode
      }
    }
  },
  components: {mListBox, mDatasource, mLocalParams, mStatementList, mSelectInput}
}
</script>
