// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addPictureToAlbum POST /api/picture/album/add */
export async function addPictureToAlbumUsingPost(
  body: API.PictureFavoriteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/album/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listPictureAlbumsWithStatus GET /api/picture/album/list/${param0} */
export async function listPictureAlbumsWithStatusUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listPictureAlbumsWithStatusUsingGETParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params
  return request<API.BaseResponseListPictureAlbumVO_>(`/api/picture/album/list/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** removePictureFromAlbum POST /api/picture/album/remove */
export async function removePictureFromAlbumUsingPost(
  body: API.PictureFavoriteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/album/remove', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deletePicture POST /api/picture/delete */
export async function deletePictureUsingPost(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** editPicture POST /api/picture/edit */
export async function editPictureUsingPost(
  body: API.PictureEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/edit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** editPictureByBatch POST /api/picture/edit/batch */
export async function editPictureByBatchUsingPost(
  body: API.PictureEditByBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/edit/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** favoritePicture POST /api/picture/favorite */
export async function favoritePictureUsingPost(
  body: API.PictureFavoriteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/favorite', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getPictureById GET /api/picture/get */
export async function getPictureByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePicture_>('/api/picture/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getPictureVOById GET /api/picture/get/vo */
export async function getPictureVoByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureVOByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureVO_>('/api/picture/get/vo', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** likePicture POST /api/picture/like/${param0} */
export async function likePictureUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.likePictureUsingPOSTParams,
  options?: { [key: string]: any }
) {
  const { pictureId: param0, ...queryParams } = params
  return request<API.BaseResponseBoolean_>(`/api/picture/like/${param0}`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** listPictureVOByCursor POST /api/picture/list/cursor/vo */
export async function listPictureVoByCursorUsingPost(
  body: API.PictureCursorQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureCursorQueryVO_>('/api/picture/list/cursor/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listUserFavoritedPictures POST /api/picture/list/favorited */
export async function listUserFavoritedPicturesUsingPost(
  body: API.PictureCursorQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureCursorQueryVO_>('/api/picture/list/favorited', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listUserLikedPictures POST /api/picture/list/liked/${param0} */
export async function listUserLikedPicturesUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listUserLikedPicturesUsingPOSTParams,
  body: API.PictureCursorQueryRequest,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params
  return request<API.BaseResponsePictureCursorQueryVO_>(`/api/picture/list/liked/${param0}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** listPictureByPage POST /api/picture/list/page */
export async function listPictureByPageUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePicture_>('/api/picture/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listPictureVOByPageCache POST /api/picture/list/page/vo */
export async function listPictureVoByPageCacheUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>('/api/picture/list/page/vo', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listPictureVOByPage POST /api/picture/list/page/vo/cache */
export async function listPictureVoByPageUsingPost(
  body: API.PictureQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>('/api/picture/list/page/vo/cache', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listPublicPictures GET /api/picture/list/public */
export async function listPublicPicturesUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPictureVO_>('/api/picture/list/public', {
    method: 'GET',
    ...(options || {}),
  })
}

/** createPictureOutPaintingTask POST /api/picture/out_painting/create_task */
export async function createPictureOutPaintingTaskUsingPost(
  body: API.CreatePictureOutPaintingRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCreateOutPaintingTaskResponse_>(
    '/api/picture/out_painting/create_task',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      data: body,
      ...(options || {}),
    }
  )
}

/** getPictureOutPaintingTask GET /api/picture/out_painting/get_task */
export async function getPictureOutPaintingTaskUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureOutPaintingTaskUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseGetOutPaintingTaskResponse_>(
    '/api/picture/out_painting/get_task',
    {
      method: 'GET',
      params: {
        ...params,
      },
      ...(options || {}),
    }
  )
}

/** doPictureReview POST /api/picture/review */
export async function doPictureReviewUsingPost(
  body: API.PictureReviewRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/review', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** searchPictureByPictureIsSo POST /api/picture/search/picture/so */
export async function searchPictureByPictureIsSoUsingPost(
  body: API.SearchPictureByPictureRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSoImageSearchResult_>('/api/picture/search/picture/so', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listPictureTagCategory GET /api/picture/tag_category */
export async function listPictureTagCategoryUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponsePictureTagCategory_>('/api/picture/tag_category', {
    method: 'GET',
    ...(options || {}),
  })
}

/** createTextToImageTask POST /api/picture/text_to_image/create_task */
export async function createTextToImageTaskUsingPost(
  body: API.CreateTextToImageRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseCreateTextToImageTaskResponse_>(
    '/api/picture/text_to_image/create_task',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      data: body,
      ...(options || {}),
    }
  )
}

/** getTextToImageTask GET /api/picture/text_to_image/get_task */
export async function getTextToImageTaskUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTextToImageTaskUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseGetTextToImageTaskResponse_>(
    '/api/picture/text_to_image/get_task',
    {
      method: 'GET',
      params: {
        ...params,
      },
      ...(options || {}),
    }
  )
}

/** updatePicture POST /api/picture/update */
export async function updatePictureUsingPost(
  body: API.PictureUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/picture/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** uploadPicture POST /api/picture/upload */
export async function uploadPictureUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadPictureUsingPOSTParams,
  body: {},
  file?: File,
  options?: { [key: string]: any }
) {
  const formData = new FormData()

  if (file) {
    formData.append('file', file)
  }

  Object.keys(body).forEach((ele) => {
    const item = (body as any)[ele]

    if (item !== undefined && item !== null) {
      if (typeof item === 'object' && !(item instanceof File)) {
        if (item instanceof Array) {
          item.forEach((f) => formData.append(ele, f || ''))
        } else {
          formData.append(ele, JSON.stringify(item))
        }
      } else {
        formData.append(ele, item)
      }
    }
  })

  return request<API.BaseResponsePictureVO_>('/api/picture/upload', {
    method: 'POST',
    params: {
      ...params,
    },
    data: formData,
    requestType: 'form',
    ...(options || {}),
  })
}

/** uploadPictureByBatch POST /api/picture/upload/batch */
export async function uploadPictureByBatchUsingPost(
  body: API.PictureUploadByBatchRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseInt_>('/api/picture/upload/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** uploadPictureByUrl POST /api/picture/upload/url */
export async function uploadPictureByUrlUsingPost(
  body: API.PictureUploadRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureVO_>('/api/picture/upload/url', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
