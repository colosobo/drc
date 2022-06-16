import Vue from 'vue'
import {
  Autocomplete,
  Button,
  Cascader,
  Checkbox,
  CheckboxGroup,
  Col,
  Collapse,
  CollapseItem,
  DatePicker,
  Dialog,
  Form,
  FormItem,
  Input,
  InputNumber,
  Loading,
  Message,
  MessageBox,
  Option,
  Pagination,
  Popover,
  Progress,
  Radio,
  RadioButton,
  RadioGroup,
  Row,
  Scrollbar,
  Select,
  Slider,
  Switch,
  Table,
  TableColumn,
  Tag,
  Tabs,
  TabPane,
  Tooltip,
  Upload,
  Dropdown,
  DropdownItem,
  DropdownMenu,
  Alert,
  Tree,
  InfiniteScroll,
  Carousel,
  CarouselItem,
  Popconfirm,
  Menu,
  MenuItem,
  MenuItemGroup,
  Submenu,
  Breadcrumb,
  BreadcrumbItem,
} from 'element-ui'

// import FilterDatePicker from '@/components/DatePicker'
// import '@/components/dgTable/css/index.css'
// Vue.use(FilterDatePicker)
Vue.use(Submenu)
Vue.use(Menu)
Vue.use(MenuItem)
Vue.use(MenuItemGroup)
Vue.use(Autocomplete)
Vue.use(Button)
Vue.use(Tag)
Vue.use(Col)
Vue.use(Radio)
Vue.use(RadioGroup)
Vue.use(DatePicker)
Vue.use(Form)
Vue.use(FormItem)
Vue.use(Select)
Vue.use(Option)
Vue.use(Input)
Vue.use(InputNumber)
Vue.use(Checkbox)
Vue.use(CheckboxGroup)
Vue.use(Loading.directive)
Vue.use(Tooltip)
Vue.use(Dialog)
Vue.use(Cascader)
Vue.use(Table)
Vue.use(TableColumn)
Vue.use(Tabs)
Vue.use(TabPane)
Vue.use(Pagination)
Vue.use(Upload)
Vue.use(Scrollbar)
Vue.use(Radio)
Vue.use(RadioGroup)
Vue.use(RadioButton)
Vue.use(Switch)
Vue.use(Row)
Vue.use(Popover)
Vue.use(Progress)
Vue.use(Slider)
Vue.use(Collapse)
Vue.use(CollapseItem)
Vue.use(Dropdown)
Vue.use(DropdownItem)
Vue.use(DropdownMenu)
Vue.use(Alert)
Vue.use(Tree)
Vue.use(InfiniteScroll)
Vue.use(Carousel)
Vue.use(CarouselItem)
Vue.use(Popconfirm)
Vue.use(Breadcrumb)
Vue.use(BreadcrumbItem)

Vue.prototype.$ELEMENT = {
  zIndex: 3000,
}
Vue.prototype.$loading = Loading.service
Vue.prototype.$msgbox = MessageBox
Vue.prototype.$alert = MessageBox.alert
Vue.prototype.$confirm = MessageBox.confirm
Vue.prototype.$prompt = MessageBox.prompt
Vue.prototype.$message = Message
