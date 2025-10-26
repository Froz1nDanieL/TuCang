// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** addPictureAlbum POST /api/pictureAlbum/add */
export async function addPictureAlbumUsingPost(
  body: API.PictureAlbumAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/pictureAlbum/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deletePictureAlbum POST /api/pictureAlbum/delete */
export async function deletePictureAlbumUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deletePictureAlbumUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/pictureAlbum/delete', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** editPictureAlbum POST /api/pictureAlbum/edit */
export async function editPictureAlbumUsingPost(
  body: API.PictureAlbumUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/pictureAlbum/edit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getFeaturedAlbums GET /api/pictureAlbum/featured */
export async function getFeaturedAlbumsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getFeaturedAlbumsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPictureAlbum_>('/api/pictureAlbum/featured', {
    method: 'GET',
    params: {
      // limit has a default value: 10
      limit: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** getPictureAlbumById GET /api/pictureAlbum/get */
export async function getPictureAlbumByIdUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getPictureAlbumByIdUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureAlbum_>('/api/pictureAlbum/get', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listSystemHotPictureAlbums GET /api/pictureAlbum/hot/system */
export async function listSystemHotPictureAlbumsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listSystemHotPictureAlbumsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPictureAlbum_>('/api/pictureAlbum/hot/system', {
    method: 'GET',
    params: {
      // limit has a default value: 10
      limit: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** listMyPictureAlbums GET /api/pictureAlbum/list/my */
export async function listMyPictureAlbumsUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseListPictureAlbum_>('/api/pictureAlbum/list/my', {
    method: 'GET',
    ...(options || {}),
  })
}

/** listPictureAlbumByPage GET /api/pictureAlbum/list/page */
export async function listPictureAlbumByPageUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listPictureAlbumByPageUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureAlbum_>('/api/pictureAlbum/list/page', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // pageSize has a default value: 10
      pageSize: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** listPicturesInAlbum GET /api/pictureAlbum/list/pictures */
export async function listPicturesInAlbumUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listPicturesInAlbumUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePagePictureVO_>('/api/pictureAlbum/list/pictures', {
    method: 'GET',
    params: {
      // current has a default value: 1
      current: '1',
      // pageSize has a default value: 10
      pageSize: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** listPicturesInAlbumByCursor GET /api/pictureAlbum/list/pictures/cursor */
export async function listPicturesInAlbumByCursorUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listPicturesInAlbumByCursorUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePictureCursorQueryVO_>('/api/pictureAlbum/list/pictures/cursor', {
    method: 'GET',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** listUserPublicAlbums GET /api/pictureAlbum/list/user/${param0} */
export async function listUserPublicAlbumsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.listUserPublicAlbumsUsingGETParams,
  options?: { [key: string]: any }
) {
  const { userId: param0, ...queryParams } = params
  return request<API.BaseResponseListPictureAlbum_>(`/api/pictureAlbum/list/user/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** getRecommendedAlbums GET /api/pictureAlbum/recommend */
export async function getRecommendedAlbumsUsingGet(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getRecommendedAlbumsUsingGETParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListPictureAlbum_>('/api/pictureAlbum/recommend', {
    method: 'GET',
    params: {
      // limit has a default value: 10
      limit: '10',
      ...params,
    },
    ...(options || {}),
  })
}

/** updatePictureAlbum POST /api/pictureAlbum/update */
export async function updatePictureAlbumUsingPost(
  body: API.PictureAlbumUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/pictureAlbum/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
