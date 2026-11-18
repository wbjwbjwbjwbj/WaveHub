import test from 'node:test'
import assert from 'node:assert/strict'
import { validateParameters, validateTransition } from '../src/services/domain.js'
test('task state transitions respect API contract', () => {
  for (const state of ['created', 'stopped'])
    assert.equal(validateTransition(state, 'start'), 'running')
  assert.equal(validateTransition('running', 'stop'), 'stopped')
  for (const state of ['running', 'failed', 'succeeded'])
    assert.throws(() => validateTransition(state, 'start'))
  for (const state of ['created', 'stopped', 'failed', 'succeeded'])
    assert.throws(() => validateTransition(state, 'stop'))
})
test('model parameters enforce required values, types, boundaries, choices and unknown keys', () => {
  const defs = [
    { key: 'n', label: '窗口', type: 'integer', required: true, min: 1, max: 10 },
    { key: 'b', label: '开关', type: 'boolean' },
    { key: 's', label: '算法', type: 'select', options: ['a', 'b'] },
  ]
  assert.doesNotThrow(() => validateParameters(defs, { n: 1, b: false, s: 'a' }))
  assert.doesNotThrow(() => validateParameters(defs, { n: 10 }))
  for (const values of [
    {},
    { n: 0 },
    { n: 11 },
    { n: 1.5 },
    { n: '2' },
    { n: NaN },
    { n: 2, b: 'false' },
    { n: 2, s: 'c' },
    { n: 2, unknown: 1 },
  ])
    assert.throws(() => validateParameters(defs, values))
})
