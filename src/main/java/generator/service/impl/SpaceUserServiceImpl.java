package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import generator.domain.SpaceUser;
import generator.service.SpaceUserService;
import generator.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

/**
* @author Danie
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-08-14 16:03:16
*/
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

}




