declare namespace API {
  type addSpaceUsingPOSTParams = {
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
  }

  type AiGenHistoryVO = {
    createTime?: string
    id?: number
    imageUrlList?: string[]
    prompt?: string
    status?: number
    taskId?: string
    userId?: number
  }

  type BaseResponseBoolean_ = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseCreateOutPaintingTaskResponse_ = {
    code?: number
    data?: CreateOutPaintingTaskResponse
    message?: string
  }

  type BaseResponseCreateTextToImageTaskResponse_ = {
    code?: number
    data?: CreateTextToImageTaskResponse
    message?: string
  }

  type BaseResponseGetOutPaintingTaskResponse_ = {
    code?: number
    data?: GetOutPaintingTaskResponse
    message?: string
  }

  type BaseResponseGetTextToImageTaskResponse_ = {
    code?: number
    data?: GetTextToImageTaskResponse
    message?: string
  }

  type BaseResponseInt_ = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponseListAiGenHistoryVO_ = {
    code?: number
    data?: AiGenHistoryVO[]
    message?: string
  }

  type BaseResponseListPictureAlbum_ = {
    code?: number
    data?: PictureAlbum[]
    message?: string
  }

  type BaseResponseListPictureAlbumVO_ = {
    code?: number
    data?: PictureAlbumVO[]
    message?: string
  }

  type BaseResponseListPictureVO_ = {
    code?: number
    data?: PictureVO[]
    message?: string
  }

  type BaseResponseListSoImageSearchResult_ = {
    code?: number
    data?: SoImageSearchResult[]
    message?: string
  }

  type BaseResponseListSpace_ = {
    code?: number
    data?: Space[]
    message?: string
  }

  type BaseResponseListSpaceCategoryAnalyzeResponse_ = {
    code?: number
    data?: SpaceCategoryAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceLevel_ = {
    code?: number
    data?: SpaceLevel[]
    message?: string
  }

  type BaseResponseListSpaceSizeAnalyzeResponse_ = {
    code?: number
    data?: SpaceSizeAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceTagAnalyzeResponse_ = {
    code?: number
    data?: SpaceTagAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceUserAnalyzeResponse_ = {
    code?: number
    data?: SpaceUserAnalyzeResponse[]
    message?: string
  }

  type BaseResponseListSpaceUserVO_ = {
    code?: number
    data?: SpaceUserVO[]
    message?: string
  }

  type BaseResponseLoginUserVO_ = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponseLong_ = {
    code?: number
    data?: number
    message?: string
  }

  type BaseResponsePagePicture_ = {
    code?: number
    data?: PagePicture_
    message?: string
  }

  type BaseResponsePagePictureAlbum_ = {
    code?: number
    data?: PagePictureAlbum_
    message?: string
  }

  type BaseResponsePagePictureVO_ = {
    code?: number
    data?: PagePictureVO_
    message?: string
  }

  type BaseResponsePageSpace_ = {
    code?: number
    data?: PageSpace_
    message?: string
  }

  type BaseResponsePageSpaceVO_ = {
    code?: number
    data?: PageSpaceVO_
    message?: string
  }

  type BaseResponsePageUserVO_ = {
    code?: number
    data?: PageUserVO_
    message?: string
  }

  type BaseResponsePicture_ = {
    code?: number
    data?: Picture
    message?: string
  }

  type BaseResponsePictureAlbum_ = {
    code?: number
    data?: PictureAlbum
    message?: string
  }

  type BaseResponsePictureCursorQueryVO_ = {
    code?: number
    data?: PictureCursorQueryVO
    message?: string
  }

  type BaseResponsePictureTagCategory_ = {
    code?: number
    data?: PictureTagCategory
    message?: string
  }

  type BaseResponsePictureVO_ = {
    code?: number
    data?: PictureVO
    message?: string
  }

  type BaseResponseSpace_ = {
    code?: number
    data?: Space
    message?: string
  }

  type BaseResponseSpaceUsageAnalyzeResponse_ = {
    code?: number
    data?: SpaceUsageAnalyzeResponse
    message?: string
  }

  type BaseResponseSpaceUser_ = {
    code?: number
    data?: SpaceUser
    message?: string
  }

  type BaseResponseSpaceVO_ = {
    code?: number
    data?: SpaceVO
    message?: string
  }

  type BaseResponseString_ = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseUser_ = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserCursorQueryVO_ = {
    code?: number
    data?: UserCursorQueryVO
    message?: string
  }

  type BaseResponseUserVO_ = {
    code?: number
    data?: UserVO
    message?: string
  }

  type CreateOutPaintingTaskResponse = {
    code?: string
    message?: string
    output?: Output
    requestId?: string
  }

  type CreatePictureOutPaintingRequest = {
    parameters?: Parameters
    pictureId?: number
  }

  type CreateTextToImageRequest = {
    n?: number
    negativePrompt?: string
    prompt?: string
    promptExtend?: boolean
    seed?: number
    size?: string
    watermark?: boolean
  }

  type CreateTextToImageTaskResponse = {
    code?: string
    message?: string
    output?: Output1
    requestId?: string
  }

  type deletePictureAlbumUsingPOSTParams = {
    /** id */
    id: number
  }

  type DeleteRequest = {
    id?: number
  }

  type getActiveUserRankingUsingGETParams = {
    /** cursorId */
    cursorId?: number
    /** pageSize */
    pageSize?: number
  }

  type getFeaturedAlbumsUsingGETParams = {
    /** limit */
    limit?: number
  }

  type GetOutPaintingTaskResponse = {
    output?: Output2
    requestId?: string
  }

  type getPictureAlbumByIdUsingGETParams = {
    /** id */
    id: number
  }

  type getPictureByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getPictureOutPaintingTaskUsingGETParams = {
    /** taskId */
    taskId?: string
  }

  type getPictureVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getPopularUserRankingUsingGETParams = {
    /** cursorId */
    cursorId?: number
    /** pageSize */
    pageSize?: number
  }

  type getRecommendedAlbumsUsingGETParams = {
    /** limit */
    limit?: number
  }

  type getSpaceByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getSpaceVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type GetTextToImageTaskResponse = {
    code?: string
    message?: string
    output?: Output3
    requestId?: string
  }

  type getTextToImageTaskUsingGETParams = {
    /** taskId */
    taskId?: string
  }

  type getUserByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type getUserVOByIdUsingGETParams = {
    /** id */
    id?: number
  }

  type likePictureUsingPOSTParams = {
    /** pictureId */
    pictureId: number
  }

  type listPictureAlbumByPageUsingGETParams = {
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
  }

  type listPictureAlbumsWithStatusUsingGETParams = {
    /** pictureId */
    pictureId: number
  }

  type listPicturesInAlbumByCursorUsingGETParams = {
    category?: string
    current?: number
    cursorId?: number
    endEditTime?: string
    id?: number
    introduction?: string
    name?: string
    nullSpaceId?: boolean
    pageSize?: number
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    reviewMessage?: string
    reviewStatus?: number
    reviewerId?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    spaceId?: number
    startEditTime?: string
    tags?: string[]
    userId?: number
    /** albumId */
    albumId: number
  }

  type listPicturesInAlbumUsingGETParams = {
    /** albumId */
    albumId: number
    /** current */
    current?: number
    /** pageSize */
    pageSize?: number
  }

  type listSystemHotPictureAlbumsUsingGETParams = {
    /** limit */
    limit?: number
  }

  type listUserLikedPicturesUsingPOSTParams = {
    /** userId */
    userId: number
  }

  type listUserPublicAlbumsUsingGETParams = {
    /** userId */
    userId: number
  }

  type LoginUserVO = {
    createTime?: string
    editTime?: string
    id?: number
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type Output = {
    taskId?: string
    taskStatus?: string
  }

  type Output1 = {
    taskId?: string
    taskStatus?: string
  }

  type Output2 = {
    code?: string
    endTime?: string
    message?: string
    outputImageUrl?: string
    scheduledTime?: string
    submitTime?: string
    taskId?: string
    taskMetrics?: TaskMetrics
    taskStatus?: string
  }

  type Output3 = {
    endTime?: string
    results?: Result[]
    scheduledTime?: string
    submitTime?: string
    taskId?: string
    taskMetrics?: TaskMetrics1
    taskStatus?: string
  }

  type PagePicture_ = {
    current?: number
    pages?: number
    records?: Picture[]
    size?: number
    total?: number
  }

  type PagePictureAlbum_ = {
    current?: number
    pages?: number
    records?: PictureAlbum[]
    size?: number
    total?: number
  }

  type PagePictureVO_ = {
    current?: number
    pages?: number
    records?: PictureVO[]
    size?: number
    total?: number
  }

  type PageSpace_ = {
    current?: number
    pages?: number
    records?: Space[]
    size?: number
    total?: number
  }

  type PageSpaceVO_ = {
    current?: number
    pages?: number
    records?: SpaceVO[]
    size?: number
    total?: number
  }

  type PageUserVO_ = {
    current?: number
    pages?: number
    records?: UserVO[]
    size?: number
    total?: number
  }

  type Parameters = {
    addWatermark?: boolean
    angle?: number
    bestQuality?: boolean
    bottomOffset?: number
    leftOffset?: number
    limitImageSize?: boolean
    outputRatio?: string
    rightOffset?: number
    topOffset?: number
    xScale?: number
    yScale?: number
  }

  type Picture = {
    category?: string
    createTime?: string
    editTime?: string
    favoriteCount?: number
    id?: number
    introduction?: string
    isDelete?: number
    likeCount?: number
    name?: string
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    reviewMessage?: string
    reviewStatus?: number
    reviewTime?: string
    reviewerId?: number
    spaceId?: number
    tags?: string
    thumbnailUrl?: string
    updateTime?: string
    url?: string
    userId?: number
  }

  type PictureAlbum = {
    createTime?: string
    description?: string
    id?: number
    isPublic?: number
    name?: string
    pictureCount?: number
    userId?: number
    viewCount?: number
  }

  type PictureAlbumAddRequest = {
    description?: string
    isPublic?: number
    name?: string
  }

  type PictureAlbumUpdateRequest = {
    description?: string
    id?: number
    isPublic?: number
    name?: string
  }

  type PictureAlbumVO = {
    createTime?: string
    description?: string
    id?: number
    isPictureFavorited?: boolean
    isPublic?: number
    name?: string
    pictureCount?: number
    userId?: number
    viewCount?: number
  }

  type PictureCursorQueryRequest = {
    category?: string
    current?: number
    cursorId?: number
    endEditTime?: string
    id?: number
    introduction?: string
    name?: string
    nullSpaceId?: boolean
    pageSize?: number
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    reviewMessage?: string
    reviewStatus?: number
    reviewerId?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    spaceId?: number
    startEditTime?: string
    tags?: string[]
    userId?: number
  }

  type PictureCursorQueryVO = {
    hasMore?: boolean
    nextCursorId?: number
    pictureList?: PictureVO[]
  }

  type PictureEditByBatchRequest = {
    category?: string
    nameRule?: string
    pictureIdList?: number[]
    spaceId?: number
    tags?: string[]
  }

  type PictureEditRequest = {
    category?: string
    id?: number
    introduction?: string
    name?: string
    tags?: string[]
  }

  type PictureFavoriteRequest = {
    albumId?: number
    pictureId?: number
  }

  type PictureQueryRequest = {
    category?: string
    current?: number
    endEditTime?: string
    id?: number
    introduction?: string
    name?: string
    nullSpaceId?: boolean
    pageSize?: number
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    reviewMessage?: string
    reviewStatus?: number
    reviewerId?: number
    searchText?: string
    sortField?: string
    sortOrder?: string
    spaceId?: number
    startEditTime?: string
    tags?: string[]
    userId?: number
  }

  type PictureReviewRequest = {
    id?: number
    reviewMessage?: string
    reviewStatus?: number
  }

  type PictureTagCategory = {
    categoryList?: string[]
    tagList?: string[]
  }

  type PictureUpdateRequest = {
    category?: string
    id?: number
    introduction?: string
    name?: string
    tags?: string[]
  }

  type PictureUploadByBatchRequest = {
    count?: number
    namePrefix?: string
    searchText?: string
  }

  type PictureUploadRequest = {
    fileUrl?: string
    id?: number
    picName?: string
    spaceId?: number
  }

  type PictureVO = {
    category?: string
    createTime?: string
    editTime?: string
    favoriteCount?: number
    id?: number
    introduction?: string
    isFavorited?: boolean
    isLiked?: boolean
    likeCount?: number
    name?: string
    permissionList?: string[]
    picFormat?: string
    picHeight?: number
    picScale?: number
    picSize?: number
    picWidth?: number
    spaceId?: number
    tags?: string[]
    thumbnailUrl?: string
    updateTime?: string
    url?: string
    user?: UserVO
    userId?: number
  }

  type Result = {
    actualPrompt?: string
    imageId?: string
    origPrompt?: string
    seed?: number
    url?: string
  }

  type SearchPictureByPictureRequest = {
    pictureId?: number
  }

  type SoImageSearchResult = {
    http?: string
    https?: string
    imgUrl?: string
    imgkey?: string
    title?: string
  }

  type Space = {
    createTime?: string
    editTime?: string
    id?: number
    isDelete?: number
    maxCount?: number
    maxSize?: number
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    userId?: number
  }

  type SpaceCategoryAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceCategoryAnalyzeResponse = {
    category?: string
    count?: number
    totalSize?: number
  }

  type SpaceEditRequest = {
    id?: number
    spaceName?: string
  }

  type SpaceLevel = {
    maxCount?: number
    maxSize?: number
    text?: string
    value?: number
  }

  type SpaceQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    userId?: number
  }

  type SpaceRankAnalyzeRequest = {
    topN?: number
  }

  type SpaceSizeAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceSizeAnalyzeResponse = {
    count?: number
    sizeRange?: string
  }

  type SpaceTagAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceTagAnalyzeResponse = {
    count?: number
    tag?: string
  }

  type SpaceUpdateRequest = {
    id?: number
    maxCount?: number
    maxSize?: number
    spaceLevel?: number
    spaceName?: string
  }

  type SpaceUsageAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
  }

  type SpaceUsageAnalyzeResponse = {
    countUsageRatio?: number
    maxCount?: number
    maxSize?: number
    sizeUsageRatio?: number
    usedCount?: number
    usedSize?: number
  }

  type SpaceUser = {
    createTime?: string
    id?: number
    spaceId?: number
    spaceRole?: string
    updateTime?: string
    userId?: number
  }

  type SpaceUserAddRequest = {
    spaceId?: number
    spaceRole?: string
    userId?: string
  }

  type SpaceUserAnalyzeRequest = {
    queryAll?: boolean
    queryPublic?: boolean
    spaceId?: number
    timeDimension?: string
    userId?: number
  }

  type SpaceUserAnalyzeResponse = {
    count?: number
    period?: string
  }

  type SpaceUserEditRequest = {
    id?: number
    spaceRole?: string
  }

  type SpaceUserQueryRequest = {
    id?: number
    spaceId?: number
    spaceRole?: string
    userId?: number
  }

  type SpaceUserVO = {
    createTime?: string
    id?: number
    space?: SpaceVO
    spaceId?: number
    spaceRole?: string
    updateTime?: string
    user?: UserVO
    userId?: number
  }

  type SpaceVO = {
    createTime?: string
    editTime?: string
    id?: number
    maxCount?: number
    maxSize?: number
    permissionList?: string[]
    spaceLevel?: number
    spaceName?: string
    spaceType?: number
    totalCount?: number
    totalSize?: number
    updateTime?: string
    user?: UserVO
    userId?: number
  }

  type TaskMetrics = {
    failed?: number
    succeeded?: number
    total?: number
  }

  type TaskMetrics1 = {
    failed?: number
    succeeded?: number
    total?: number
  }

  type testDownloadFileUsingGETParams = {
    /** filepath */
    filepath?: string
  }

  type uploadPictureUsingPOSTParams = {
    fileUrl?: string
    id?: number
    picName?: string
    spaceId?: number
  }

  type User = {
    createTime?: string
    editTime?: string
    id?: number
    isDelete?: number
    updateTime?: string
    userAccount?: string
    userAvatar?: string
    userName?: string
    userPassword?: string
    userProfile?: string
    userRole?: string
  }

  type UserAddRequest = {
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserCursorQueryVO = {
    hasMore?: boolean
    nextCursorId?: number
    userList?: UserVO[]
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    current?: number
    id?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    userAccount?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    checkPassword?: string
    userAccount?: string
    userPassword?: string
  }

  type UserUpdateRequest = {
    id?: number
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    createTime?: string
    heatValue?: number
    id?: number
    pictureCount?: number
    userAccount?: string
    userAvatar?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }
}
