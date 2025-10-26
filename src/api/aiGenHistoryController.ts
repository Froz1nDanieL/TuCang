// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** listUserAiGenHistories GET /api/aiGenHistory/list */
export async function listUserAiGenHistoriesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListAiGenHistoryVO_>('/api/aiGenHistory/list', {
    method: 'GET',
    ...(options || {}),
  })
}
