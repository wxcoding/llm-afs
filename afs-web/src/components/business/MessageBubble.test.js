import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import MessageBubble from './MessageBubble.vue'

const mountMessage = (msg) =>
  mount(MessageBubble, {
    props: { msg },
    global: {
      plugins: [createPinia()],
      stubs: { ElAvatar: true, ElIcon: true, ElTag: true }
    }
  })

describe('MessageBubble', () => {
  it('warns users when an answer has no trusted knowledge-base evidence', () => {
    const wrapper = mountMessage({
      role: 'assistant',
      content: '请谨慎核实。',
      sources: [],
      noEvidence: true,
      degraded: false
    })

    expect(wrapper.text()).toContain('未检索到可信的知识库依据')
    expect(wrapper.text()).toContain('拨打 96110 核实')
  })

  it('does not show the no-evidence warning when citations are available', () => {
    const wrapper = mountMessage({
      role: 'assistant',
      content: '请查看来源。',
      sources: [{ title: '反诈指南', type: 'knowledge', score: 0.9 }],
      noEvidence: false,
      degraded: false
    })

    expect(wrapper.text()).toContain('反诈指南')
    expect(wrapper.text()).not.toContain('未检索到可信的知识库依据')
  })
})
